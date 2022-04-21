# rbt

Utility which compiles separate Markdown files with requirements into one software requirements document.

Features:

1. Checks definied IDs uniqueness.
2. Checks that referenced ID exists.
3. Allows referencing to the requirement's header by its ID.
4. Allows building the dependency tree.

## Usage

You need a 'reqs.edn' file inside your project directory. Example:

	{:src "mdsrc"
	 :document "doc.md"
	 :files ["ucs.md", "ui.md"]
	}

* :src -- the source directory where the Markdown files are placed.
* :document -- the resulting file name containing compiled Markdown files.
* :files -- list of Markdown files to be compiled. The order of the files will be preserved.

Run the 'rbt' tool as a normal jar inside the project directory where your 'reqs.edn' file exists.

	$ java -jar rbt-0.1.0-SNAPSHOT-standalone.jar 

Supported commands:

	no command -- build the project
	-d REQ_IDS -- show the requirements which depend on the specified IDs (separated by comma)
	-h -- show help

Supported options:

	-p PROJECT_FILE -- specify this option if you need project file name other than 'reqs.edn'.


### Supported types of defined IDs

1. Header IDs in square brackets:

        ## [MY-ID] My requirement header

In this case the ID of the requirement will by 'MY-ID'.

2. Complex format:

	    #id:{01:Button title}

This is a local ID definition to be used in lists or tables. If defined within the requiement with ID like previous one, the resulting ID will be 'MY-ID/01'.


### Supported types of references

1. #refId:ID -- refrence text will be its ID.

Example: #refId:MY-ID will be transformed to [MY-ID](#MY-ID).

2. #refText:ID -- refrence text will be its text.

Example: #refText:MY-ID will be transformed to [My requirement header](#MY-ID).

3. #ref:ID -- reference text will be the combination of its ID and text: '[ID] Text'.

Example: #ref:MY-ID will be transformed to [MY-ID: My requirement header](#MY-ID).

4. #refsTo:ID -- lists all references to this requirement.

5. #refsFrom:ID -- lists all references from this requirement.

### Dependency tree

You may check the dependency tree of an individual (or list of comma-separated) requirement like this:

	java -jar ../target/rbt-0.1.0-SNAPSHOT-standalone.jar -d UI-01

The result will be:

	Requirement UI-01
	~~~~~~~~~~~~~~~~~~~~
	Dependency tree:
	
	UI-01
	├─UCS-02
	└─UCS-01
	  └─UCS-02
	
	Dependent requirements list:
	UCS-01 UCS-02


### Compare specifications

If you need to see the difference betwee the two specification, specify them with options '-p' and '-c':

	java -jar ../target/rbt-0.1.0-SNAPSHOT-standalone.jar -p example1/reqs.edn -c example2/reqs.edn

Comparison will give you the something like this:

	Removed requirements:
	[]

	Added requirements:
	[UI-03]

	Different requirements:
	[UCS-02]


## Example

The directory 'example' contains small example with the source requirements defined in directory 'example/src/' and corresponding project file 'example/reqs.edn'.

The script 'example/run.sh' shows how the user may build the software requirements document and convert it to different formats. Bear in mind that it uses pandoc utility which should be installed. And pandoc itself uses latex to convert to pdf, so latex also needed for that.

Converting to docx format is not accurate, after converting the user must fix some styles for tables.

Examples of the software requirements document in different formats are in the 'examples/target' directory.

## License

Copyright © 2022 Dmitry Bushenko (d.bushenko@gmail.com)

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
