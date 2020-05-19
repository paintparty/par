# par

A Clojure(Script) library designed to print-and-return values.

It will print what you are evaluating, and the result of evaluating it.

Intended to wrap existing values in your source code so they can be observed without changing the execution of the program.

## Usage

Add as a dependency to your project:

```clojure
[par "0.1.0"]
```

And setup your namespace imports:

```clojure
(ns my-cljs-ns
  (:require
    [par.core :refer-macros [? ?c]]))
```


## Printing

The `?` and `?c` macros give you `js/console.log` and `println`, respectively. Use `?c` if you are in a `.clj`, or `.cljc` file.

```clojure
(? (+ 1 2)) ; => 3

;; The above will print the following to js/console.log:
(+ 1 2)
=> 3
```


## License

Copyright © 2020 JC

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
