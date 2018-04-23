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
 /*global require*/
 const _ = require('underscore');
 const $ = require('jquery');
 const Backbone = require('backbone');
 const ResultForm = require('../search-form');
 const Common = require('js/Common');
 const user = require('component/singletons/user-instance');

 let resultTemplates = [];
 const templatePromise = $.ajax({
    type: 'GET',
    context: this,
    url: '/search/catalog/internal/forms/result',
    contentType: 'application/json',
    success: function (data) {
        resultTemplates = data;
    }
});

 module.exports = Backbone.AssociatedModel.extend({
   model: ResultForm,
   defaults: {
        doneLoading: false,
        resultForms: []
   },
   initialize: function() {
        this.addResultForm(new ResultForm({
            name: 'Create New',
            type: 'result',
            createdOn: ""
        }));
        this.addResultForms();
   },
    relations: [{
        type: Backbone.Many,
        key: 'resultForms',
        collectionType: Backbone.Collection.extend({
            model: ResultForm,
            initialize: function() {}
        })
    }],
   addResultForms: function() {
       templatePromise.then(() => {
            if (!this.isDestroyed){
                $.each(resultTemplates, (index, value) => {
                    let utcSeconds = value.created / 1000;
                    let d = new Date(0);
                    d.setUTCSeconds(utcSeconds);
                    this.addResultForm(new ResultForm({
                        createdOn: Common.getHumanReadableDate(d),
                        id: value.id,
                        name: value.title,
                        type: 'result',
                        descriptors: value.descriptors,
                        accessIndividuals: value.accessIndividuals,
                        accessGroups: value.accessGroups,
                        createdBy: value.creator
                    }));
                });
                this.doneLoading();
            }
       });
   },
   checkIfOwnerOrSystem: function(template) {
    let myEmail = user.get('user').get('email');
    let templateCreator = template.creator;
    return myEmail === templateCreator || templateCreator === "System Template";
    },
    addResultForm: function(newForm) {
        this.get('resultForms').add(newForm);
    },
    getDoneLoading: function() {
        return this.get('doneLoading');
    },
    doneLoading: function() {
        this.set('doneLoading', true);
    },
    getCollection: function() {
        return this.get('resultForms');
    },
 });