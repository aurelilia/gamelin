#!/usr/bin/env bash

# For some reason KORGE does not properly bundle index.html,
# it's instead hardcoded from another project as I cannot be bothered
# spending any more time on fixing this...

gradle jsBrowserWebpack
cp assets/web/index.html build/distributions/
xdg-open build/distributions/index.html