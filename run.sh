#!/bin/sh
java -jar target/rbt-0.1.0-SNAPSHOT-standalone.jar -p example/reqs.edn
pandoc example/target/srs.md -o example/target/srs.pdf
pandoc example/target/srs.md -o example/target/srs.html
pandoc example/target/srs.md -o example/target/srs.docx
