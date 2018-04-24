/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define, setTimeout*/
const Marionette = require('marionette');
const _ = require('underscore');
const $ = require('jquery');
const template = require('./query-result.hbs');
const CustomElements = require('js/CustomElements');
const store = require('js/store');
const PropertyView = require('component/property/property.view');
const Property = require('component/property/property');
const metacardDefinitions = require('component/singletons/metacard-definitions');
const Loading = require('component/loading-companion/loading-companion.view');


module.exports = Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('query-result'),
        modelEvents: {},
        events: {
            'click .editor-edit': 'edit',
            'click .editor-cancel': 'cancel',
            'click .editor-save': 'save'
        },
        regions: {
            basicTitle: '.basic-text',
            basicAttribute: '.basic-type',
            basicAttributeSpecific: '.basic-type-specific'
        },
        ui: {},
        filter: undefined,
        onBeforeShow: function(){
            this.model = this.model._cloneOf ? store.getQueryById(this.model._cloneOf) : this.model;
            this.setupTitleInput();
            this.setupAttribute();
            this.setupAttributeSpecific();
            this.listenTo(this.basicAttribute.currentView.model, 'change:value', this.handleAttributeValue);
            this.handleAttributeValue();
            this.turnOnLimitedWidth();
            this.edit();
        },
        setupAttributeSpecific: function(){
            let currentValue = this.model.get('template').descriptors !== '{}' ? this.model.get('template').descriptors : [];
            this.basicAttributeSpecific.show(new PropertyView({
                model: new Property({
                    enumFiltering: true,
                    showValidationIssues: false,
                    enumMulti: true,
                    enum: metacardDefinitions.sortedMetacardTypes.map(function(metacardType){
                        return {
                            label: metacardType.alias || metacardType.id,
                            value: metacardType.id
                        };
                    }),
                    values: this.model.get('descriptors'),
                    value: [currentValue],
                    id: 'Attributes'
                })
            }));
        },
        setupAttribute: function () {
            //is there a better way to check for this?
            let currentValue = this.model.get('template').descriptors !== '{}' ? 'specific' : 'any';
            this.basicAttribute.show(new PropertyView({
                model: new Property({
                    value: [currentValue],
                    id: 'Match Attributes',
                    radio: [{
                        label: 'All',
                        value: 'any'
                    }, {
                        label: 'Specific',
                        value: 'specific'
                    }]
                })
            }));
        },
        setupTitleInput: function () {
            let currentValue = this.model.get('template').name ? this.model.get('template').name : '';
            this.basicTitle.show(new PropertyView({
                model: new Property({
                    value: [currentValue],
                    id: 'Title',
                    placeholder: 'Result Form Title'
                })
            }));
        },
        handleAttributeValue: function () {
            var attribute = this.basicAttribute.currentView.model.getValue()[0];
            this.$el.toggleClass('is-type-any', attribute === 'any');
            this.$el.toggleClass('is-type-specific', attribute === 'specific');
        },
        turnOnLimitedWidth: function () {
            this.regionManager.forEach(function (region) {
                if (region.currentView && region.currentView.turnOnLimitedWidth) {
                    region.currentView.turnOnLimitedWidth();
                }
            });
        },
        turnOffEdit: function () {
            this.regionManager.forEach(function (region) {
                if (region.currentView && region.currentView.turnOffEditing) {
                    region.currentView.turnOffEditing();
                }
            });
        },
        edit: function () {
            this.$el.addClass('is-editing');
            this.regionManager.forEach(function (region) {
                if (region.currentView && region.currentView.turnOnEditing) {
                    region.currentView.turnOnEditing();
                }
            });
            var tabbable = _.filter(this.$el.find('[tabindex], input, button'), function (element) {
                return element.offsetParent !== null;
            });
            if (tabbable.length > 0) {
                $(tabbable[0]).focus();
            }
        },
        focus: function(){
            this.basicText.currentView.focus();
        },
        cancel: function(){
            this.cleanup();
        },
        handleDownConversion: function (downConversion) {
            this.$el.toggleClass('is-down-converted', downConversion);
        },
        save: function () {
            let view = this;
            Loading.beginLoading(view);
            let descriptors = this.basicAttributeSpecific.currentView.model.get('value'); 

            let templatePerms = {
                'descriptors': descriptors
            };
            this.updateResults(templatePerms);
        },
        updateResults: function(templatePerms) {
            let resultEndpoint = `/search/catalog/internal/forms/result/${this.options.modelId}`
            $.ajax({
                url: resultEndpoint,
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                type: 'PUT',
                data: JSON.stringify(templatePerms),
                context: this,
                success: function(data) {
                    this.message('Success!', 'Saved Result Form', 'success');
                    this.cleanup();
                },
                error: this.cleanup()
            });
        },
        cleanup: function () {
            this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox');
            Loading.endLoading(this);
        }
});