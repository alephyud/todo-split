# todo-split

generated using Luminus version "2.9.12.29"

This is an example Clojure / ClojureScript todo list app, built using
[Reagent][1] / [re-frame][2] / [Kee-Frame][3].

You can view the live demo of the application
[here](https://todosplit.mydemoapps.net/).

Once you open the app, press <kbd>H</kbd> or click 'Help' for a
list of key bindings. You can split any task into subtasks
by pressing the <kbd>S</kbd> key.
You can also have a set of tasks generated for you
using Clojure Spec generators by pressing the <kbd>G</kbd> key.


The changes are persisted in the browser's local storage using
[re-frame-storage][4]
and have undo / redo enabled using [re-frame-undo][5].

[1]: https://github.com/reagent-project/reagent/
[2]: https://github.com/Day8/re-frame
[3]: https://github.com/ingesolvoll/kee-frame
[4]: https://github.com/akiroz/re-frame-storage
[5]: https://github.com/Day8/re-frame-undo

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed. If you want to change
styles and non-CLJS scripts (including the Bootstrap build),
you will also need NPM.

[1]: https://github.com/technomancy/leiningen

## Development Setup

To start a development Figwheel server for the application, run:

    lein figwheel app test
    
It also includes the web server.
After that, open `http://localhost:3000` for the application and
`http://localhost:3000/test` for frontend tests.

Styles and non-CLJS scripts are built using [Gulp][1]. To start Gulp,
install the NPM dependencies:

    npm install --dev
    npm install gulp -g

and then run Gulp:

    gulp watch

[1]: https://github.com/gulpjs/gulp

## To-do list :)

* ~~Editing and selecting states (like in Jupyter)~~
* ~~Marking tasks as done~~
* ~~Splitting tasks~~
* ~~When the last task in list is removed, move cursor upwards~~
* ~~Local storage~~
* ~~Help page~~
* ~~Instructions on generating new page and help~~
* ~~Expanding and collapsing items~~
* ~~Integration tests~~
* First deployment
* Hash-based navigation
* State on backend and sync
* DB-backed backend and accounts
* Expanded to-do item format (?)
    - timestamps
    - notes
    - images
    - attached files

## License

[Eclipse Public License](LICENSE) Copyright © 2018 AI
