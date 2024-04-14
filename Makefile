.PHONY: format docs test

java-files := $(shell find src -name '*.java')
config-files := $(shell find src -name '*.yml') build.gradle settings.gradle
version := $(shell src/build/version.py)

build: build/libs/PortalNetwork-$(version).jar
build/libs/PortalNetwork-$(version).jar: $(java-files) $(config-files)
	./gradlew build
	@echo built build/libs/PortalNetwork-Clod-$(version).jar

format: build/format
build/format: $(java-files) $(config-files)
	@mkdir -p build
	./gradlew :spotlessApply
	@touch $@

test:
	./gradlew check

docs: venv
	venv/bin/mkdocs build

venv: venv/updated
venv/updated: requirements.txt
	[ -e venv/bin/python ] || python3.11 -m venv venv
	venv/bin/pip install -U pip wheel --disable-pip-version-check
	venv/bin/pip install -U -r requirements.txt
	@touch $@
