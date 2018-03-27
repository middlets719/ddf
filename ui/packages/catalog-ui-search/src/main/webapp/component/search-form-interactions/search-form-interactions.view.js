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
/*global define, window*/



define([
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    './search-form-interactions.hbs',
    'js/CustomElements',
    'js/store',
    'component/router/router',
    'component/singletons/user-instance',
    'component/loading/loading.view',
    'component/lightbox/lightbox.view.instance',
    'component/announcement'
], function(wreqr, Marionette, _, $, template, CustomElements, store, router, user,
    LoadingView, lightboxInstance, announcement) {
    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('search-form-interactions'),
        className: 'composed-menu',
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click .interaction-trash': 'handleTrash',
            'click .search-form-interaction': 'handleClick'
        },
        ui: {},
        initialize: function() {},
        onRender: function() {
            this.checkIfSubscribed();
            //this.handleLocal();
        },
        handleLocal: function() {
            this.$el.toggleClass('is-local', this.model.isLocal());
        },
        checkIfSubscribed: function() {
            this.$el.toggleClass('is-subscribed', Boolean(this.model.get('subscribed')));
        },
        handleTrash: function() {
            var loadingview = new LoadingView();
            var loginUser = user.get('user');
            if(loginUser.attributes.username == this.model.attributes.createdBy)
            {
                $.ajax({
                    type: 'Delete',
                    context: this,
                    url: '/search/catalog/internal/forms/' + this.model.id,
                    contentType: 'application/json',
                });;
                var index = this.model;
                this.model.collection.remove(index);
            }
            else{
                announcement.announce({
                    title: 'Error!',
                    message: "Unable to delete the form: You are not the author",
                    type: 'error'
                });
                
            }
            loadingview.remove();
            this.trigger("doneLoading");
        },
        handleClick: function() {
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
        }
    });
});
