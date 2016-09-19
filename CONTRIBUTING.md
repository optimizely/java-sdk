# Contributing to the Optimizely Java SDK

We welcome contributions and feedback! Please read the [README](README.md) to set up your development environment,
then read the guidelines below for information on submitting your code.

## Development process

1. Create a fork of the `optimizely/java-sdk` repository.
2. Create a branch off of `master`: `git checkout -b YOUR_NAME/branch_name`.
3. Commit your changes. Make sure to add tests!
4. Run `./gradlew clean check` to make sure there are no possible bugs.
5. `git push` your changes to GitHub.
6. Make sure that all unit tests are passing and that there are no merge conflicts between your branch and `master`.
7. Open a pull request from `YOUR_NAME:YOUR_NAME/branch_name` to `optimizely:master`.
8. A repository maintainer will review your pull request and, if all goes well, merge it!

## Pull request acceptance criteria

* **All code must have test coverage.** We use JUnit. Changes in functionality should have accompanying unit tests. Bug fixes should have accompanying regression tests.
  * Tests are located in `core-api/src/test`.
* Version will be bumped automatically through Gradle after your change is merged.
* Make sure `./gradlew check` runs successfully before submitting.

## Style

Refer to the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

## License

By contributing your code, you agree to license your contribution under the terms of the
[Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0).
Your contributions should also include the following header:

```
/**
 * Copyright 2016, Optimizely
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 ```

## Contact

If you have questions, please contact developers@optimizely.com.
