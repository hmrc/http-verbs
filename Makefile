help: ## Print this help
	@fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' | sed -e 's/##//'

all: format build report ## run all tasks

build: ## build project with sbt
	sbt clean compile test

format: ## format scala source code
	scalafmt

report: ## html report with dependencies
	sbt ivyReport "show ivyReport"
