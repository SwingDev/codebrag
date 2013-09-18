angular.module('codebrag.common.directives')

    .directive('reactionMessageSummary', function ($filter) {
        var converter = Markdown.getSanitizingConverter();

        function removeAllTags(message) {
            return $(message).text()
        }

        return {
            restrict: 'E',
            template: '<span ng-bind-html-unsafe="reactionMessage"></span>',
            replace: true,
            scope: {
                reaction: '='
            },
            link: function (scope, el, attrs) {
                var reaction = scope.reaction;
                if (reaction.message) {
                    var formattedMessage = converter.makeHtml(reaction.message);
                    scope.reactionMessage = $filter('truncate')(removeAllTags(formattedMessage), 50);
                } else {
                    scope.reactionMessage = reaction.reactionAuthor + ' liked your code.';
                }
            }
        };
    });