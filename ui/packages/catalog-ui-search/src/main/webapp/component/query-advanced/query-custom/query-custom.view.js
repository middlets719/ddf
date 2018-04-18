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
define([
    'marionette',
    'underscore',
    'jquery',
    './query-custom.hbs',
    'js/CustomElements',
    'component/filter-builder/search-form/filter-builder.search-form.view',
    'component/filter-builder/filter-builder',
    'js/cql',
    'js/store',
    'component/property/property',
    'component/property/property.view',
    'component/query-settings/query-settings.view',
    'component/query-advanced/query-advanced.view',
    'component/result-form/result-form'
], function (Marionette, _, $, template, CustomElements, FilterBuilderView, FilterBuilderModel, cql,
            store, Property, PropertyView, QuerySettingsView, QueryAdvanced, ResultForm) {

    return QueryAdvanced.extend({
        template: template,
        regions: {
            querySettings: '.query-settings',
            queryAdvanced: '.query-advanced',
            resultTemplate: '.query-result-template'
        },
        className: 'is-custom',
        onBeforeShow: function(){
            this.model = this.model._cloneOf ? store.getQueryById(this.model._cloneOf) : this.model;
            this.querySettings.show(new QuerySettingsView({
                model: this.model
            }));
            this.queryAdvanced.show(new FilterBuilderView({
                model: new FilterBuilderModel()
            }));

            if (this.options.filterTemplate) {
                this.setCqlFromFilter(this.options.filterTemplate);
            } else if (this.model.get('cql')) {
                this.queryAdvanced.currentView.deserialize(cql.simplify(cql.read(this.model.get('cql'))));
            }

            if (ResultForm.getResultTemplatesProperties()) {
                var detailLevelProperty = new Property({
                    label: 'Detail Level',
                    enum: ResultForm.getResultTemplatesProperties(),
                    id: 'Detail Level'
                });

                this.listenTo(detailLevelProperty, 'change:value', this.handleChangeDetailLevel);
                this.resultTemplate.show(new PropertyView({
                    model: detailLevelProperty
                }));

                this.resultTemplate.currentView.turnOnLimitedWidth();
                this.resultTemplate.currentView.turnOnEditing();
            }
            this.querySettings.currentView.turnOffEditing();
            this.queryAdvanced.currentView.turnOffEditing();
            this.edit();
        },
        handleChangeDetailLevel: function(model, values) {
            $.each(model.get('enum') , (function(index, value) {
                if (values[0] === value.value) {
                    this.model.set('selectedResultTemplate', value);
                }
            }).bind(this));
        },
        edit: function(){
            this.$el.addClass('is-editing');
            this.queryAdvanced.currentView.turnOnEditing();
            this.querySettings.currentView.turnOnEditing();
        },
        setDefaultTitle: function(){
            this.model.set('title', 'Custom Title');
        },
        setCqlFromFilter: function(filterTemplate) {
            this.queryAdvanced.currentView.model.set('operator', filterTemplate.type);
            this.queryAdvanced.currentView.setFilters(filterTemplate.filters);
            let filter = this.queryAdvanced.currentView.transformToCql();
            this.model.set({
                cql: filter
            });
        }
    });
});
