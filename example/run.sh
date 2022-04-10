#!/bin/sh
java -jar ../target/rbt-0.1.0-SNAPSHOT-standalone.jar
pandoc target/srs.md -o target/srs.pdf
pandoc target/srs.md -o target/srs.html
pandoc target/srs.md -o target/srs.docx
