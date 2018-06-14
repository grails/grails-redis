#!/bin/bash

set -e

rm -rf *.zip

./grailsw test-app

echo "branch: $TRAVIS_BRANCH"
echo "pullrequest: $TRAVIS_PULL_REQUEST"
echo "travis tag: $TRAVIS_TAG"

EXIT_STATUS=0
echo "Publishing archives for branch $TRAVIS_BRANCH"
if [[ -n $TRAVIS_TAG ]] || [[ $TRAVIS_BRANCH == 'grails_2' && $TRAVIS_PULL_REQUEST == 'false' ]]; then

  echo "Publishing archives"

  if [[ -n $TRAVIS_TAG ]]; then
      ./grailsw publish-plugin || EXIT_STATUS=$?
  fi

fi

exit $EXIT_STATUS