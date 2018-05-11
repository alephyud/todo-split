gulp = require 'gulp'
concat = require 'gulp-concat'
cleanCSS = require 'gulp-clean-css'
sass = require 'gulp-sass'
rename = require 'gulp-rename'
browserSync = require('browser-sync').create()
browserify = require 'browserify'
source = require 'vinyl-source-stream'

gulp.task 'css', ->
  gulp.src 'resources/scss/*.scss'
  .pipe sass().on 'error', sass.logError
  .pipe concat 'styles.css'
  .pipe cleanCSS()
  .pipe rename {suffix: '.min'}
  .pipe gulp.dest 'resources/public/css'
  .pipe browserSync.stream()

gulp.task 'fonts', ->
  gulp.src 'node_modules/@fortawesome/fontawesome-free-webfonts/webfonts/**.*'
    .pipe gulp.dest 'resources/public/webfonts'

gulp.task 'js', ->
  browserify
    entries: ['./resources/js/main.coffee']
    extensions: ['.coffee', '.js']
  .transform 'coffeeify'
  .transform {global: true}, 'uglifyify'
  .bundle()
  .on 'error', (err) -> console.log err.toString(); @emit 'end'
  .pipe source 'scripts.min.js'
  .pipe gulp.dest 'resources/public/js'

gulp.task 'watch', ->
  gulp.watch './resources/scss/*', ['css']
  gulp.watch './resources/js/*', ['js']

gulp.task 'default', ['css', 'js', 'fonts']
