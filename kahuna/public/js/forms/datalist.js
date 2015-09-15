import angular from 'angular';

import template from './datalist.html!text';
import '../util/eq';

export var datalist = angular.module('kahuna.forms.datalist', ['util.eq']);


datalist.directive('grDatalist', [function() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            search: '&grSearch'
        },
        template: template,
        controllerAs: 'ctrl',
        controller: [function() {
            var ctrl = this;
            var selectedIndex = 0;

            ctrl.results = [];

            ctrl.moveIndex = movement =>
                selectedIndex = (selectedIndex + movement + ctrl.results.length) %
                                ctrl.results.length;

            ctrl.isSelected = key => key === selectedIndex;

            ctrl.searchFor = q =>
                ctrl.search({ q }).then(results => ctrl.results = results);

            ctrl.setValueTo = value => ctrl.value = value;

            ctrl.setValueFromSelectedIndex = () => {
                ctrl.value = ctrl.results[selectedIndex];
            };

            ctrl.reset = () => {
                selectedIndex = 0;
                ctrl.results = [];
            };
        }],
        bindToController: true
    };
}]);


datalist.directive('grDatalistInput',
                   ['$timeout', 'onValChange',
                   function($timeout, onValChange) {
    return {
        restrict: 'A',
        require:['^grDatalist', '?ngModel'],

        link: function(scope, element, _/*attrs*/, [parentCtrl, ngModel]) {
            // This feels like it should be set to this directive, but it is
            // needed in the template so we set it here.
            parentCtrl.active = false;

            const input = angular.element(element[0]);
            const keys = { 38: 'up', 40: 'down', 13: 'enter', 27: 'esc', 9: 'tab' };
            const keyFuncs = {
                up:    () => parentCtrl.moveIndex(-1),
                down:  () => parentCtrl.moveIndex(+1),
                esc:   deactivate
            };

            // Enter is on keydown to prevent the submit event being
            // propagated up.
            input.on('keydown', event => {
                if (keys[event.which] === 'enter' && parentCtrl.active) {
                    event.preventDefault();
                    scope.$apply(parentCtrl.setValueFromSelectedIndex);
                }
            });

            input.on('keyup', event => {
                const func = keyFuncs[keys[event.which]];

                if (func && parentCtrl.active) {
                    event.preventDefault();
                    scope.$apply(func);
                } else {
                    searchAndActivate();
                }
            });

            input.on('click', searchAndActivate);

            // This is done to make the results disappear when you select
            // somewhere else on the document, but still allowing you to click
            // a result. What would have been nicer would be to have looked for
            // a `document.click` and `stopPropagation`ed on the parent element.
            // Unfortunately this isn't possible as a `document.click` is fired
            // from the submit button of a form (which most forms have).
            input.on('blur', () => $timeout(deactivate, 150));

            scope.$watch(() => parentCtrl.value, onValChange(newVal => {
                ngModel.$setViewValue(newVal, 'gr:datalist:update');
                ngModel.$commitViewValue();
                ngModel.$render();

                deactivate();
            }));

            function searchAndActivate() {
                parentCtrl.searchFor(input.val()).then(activate);
            }

            function activate(results) {
                const isOnlyResult = results.length === 1 && input.val() === results[0];
                const noResults = results.length === 0 || isOnlyResult;

                parentCtrl.active = !noResults;
            }

            function deactivate() {
                parentCtrl.active = false;
                parentCtrl.reset();
            }
        }
    };
}]);
