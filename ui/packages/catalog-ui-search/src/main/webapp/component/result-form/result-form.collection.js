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
 /* global require */
 const Backbone = require('backbone')
 const ResultForm = require('component/search-form/search-form')
 const Common = require('js/Common')
 const user = require('component/singletons/user-instance')
 const $ = require('jquery')
 const _ = require('underscore')

 let resultTemplates = [];
 let promiseIsResolved = false;
 const resultTemplatePromise = () => $.ajax({
     type: 'GET',
     url: '/search/catalog/internal/forms/result',
     contentType: 'application/json',
     success: function(data) {
         //Find templates with the same id but different property maps (because we should trust the server)
         let updatedTemplates = data.filter(
             incomingTemplate => _.any(resultTemplates, (cachedTemplate) => cachedTemplate.id === incomingTemplate.id && !_.isEqual(cachedTemplate, incomingTemplate))
         );
         //Find templates that are new
         let newTemplates = data.filter(
             incomingTemplate => resultTemplates.length === 0 || !_.any(resultTemplates, (cachedTemplate) => cachedTemplate.id === incomingTemplate.id)
         );
         //Replace updated templates in their corresponding indices (//TODO: Should this just be a backbone collection instead of an array?)
         _.each(updatedTemplates, 
             updatedTemplate => resultTemplates[_.findIndex(resultTemplates, (cachedTemplate) => cachedTemplate.id === updatedTemplate.id)] = updatedTemplate
         );
         resultTemplates = resultTemplates.concat(newTemplates);
         promiseIsResolved = true;
     }
 });
 let bootstrapPromise = resultTemplatePromise();

 module.exports = Backbone.AssociatedModel.extend({
   model: ResultForm,
   defaults: {
     doneLoading: false,
     added: false,
     resultForms: []
   },
   initialize: function () {
     this.addResultForm(new ResultForm({
       name: 'Create New Data View',
       type: 'newResult'
     }))
     this.addResultForms()
   },
   relations: [{
     type: Backbone.Many,
     key: 'resultForms',
     collectionType: Backbone.Collection.extend({
       model: ResultForm,
       initialize: function () {}
     })
   }],
   addResultForms: function () {
    this.set('doneLoading',false)
    this.set('added', false)
     if (!this.isDestroyed) {
      if (promiseIsResolved === true) {
        promiseIsResolved = false;
        bootstrapPromise = new resultTemplatePromise();
      }
      bootstrapPromise.then(() => {
            this.filteredList = _.map(resultTemplates, function(resultForm) {
              return {
                  label: resultForm.title,
                  value:resultForm.title,
                  id: resultForm.id,
                  descriptors: resultForm.descriptors,
                  description: resultForm.description,
                  created: resultForm.created,
                  creator: resultForm.creator,
                  createdBy: resultForm.owner,
                  accessGroups: resultForm.accessGroups,
                  accessIndividuals: resultForm.accessIndividuals
              };
            });
            this.resetResultForm()
            this.addResultForm(new ResultForm({
              name: 'Create New Data View',
              type: 'newResult'
            }))
            this.filteredList.forEach(element => {
              let utcSeconds = element.created / 1000
              let d = new Date(0)
              d.setUTCSeconds(utcSeconds)
              this.addResultForm(new ResultForm({
                createdOn: Common.getHumanReadableDate(d),
                id: element.id,
                name: element.label,
                type: 'result',
                descriptors: element.descriptors,
                accessIndividuals: element.accessIndividuals,
                accessGroups: element.accessGroups,
                createdBy: element.createdBy,
                description: element.description
              }))
            });
            this.doneLoading();
        });
      }
   },
   checkIfOwnerOrSystem: function (template) {
     let myEmail = user.get('user').get('email')
     let templateCreator = template.creator
     return myEmail === templateCreator || templateCreator === 'System Template'
   },
   addResultForm: function (newForm) {
     this.get('resultForms').add(newForm)
   },
   resetResultForm: function () {
    this.get('resultForms').reset()
   },
   getDoneLoading: function () {
     return this.get('doneLoading')
   },
   toggleUpdate: function(){
    this.set('added', !this.get('added'))
   },
   doneLoading: function () {
     this.set('doneLoading', true)
   },
   getCollection: function () {
     return this.get('resultForms')
   },
   deleteCachedTemplateById: function (id) {
    if(this.filteredList)
    {
      this.filteredList = _.filter(this.filteredList, function(template) {
        return template.id !== id
      })
      this.toggleUpdate()
    }
    if(resultTemplates)
    {
    resultTemplates = _.filter(resultTemplates, function(template) {
      return template.id !== id
    })}
  }
 })
