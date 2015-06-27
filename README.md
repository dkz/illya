# プリズマ☆イリヤちゃん

Clojure-based sandbox web1.0 ascii-centric anonymous textboard built on top of the jetty-async server. Actually I use this project to get acquainted with clojure libraries and orient-db server graph API.

## Installation

For dev environment:

-  Download a community edition of orient-db server from http://orientdb.com/download/
-  Create a database through console using `create database` 
-  Fill right credentials in config.properties
-  `lein ring server`
-  Nrepl server is available and listening on port 40400

Launching as stand-alone jar follows exact the same steps except that `-Dillya.config=configuration.properties` required.

## Several notes

- `clj-orient` is ded (rip)
- `clj-blueprints` is ded
- `cats` is a really nice library
- Wrote my own db-migration mechanism because I can
- Do not take seriously all that is published here

## License

Copyright (c) 2015, Dmitry Kozlov
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those
of the authors and should not be interpreted as representing official policies,
either expressed or implied, of the FreeBSD Project.
