# Generate Code Documentation
## Steps

### Automatic
		
* Open terminal navigate to SDK root directory.
* In SDK root directory, execute **./gradlew build**
* This will generate HTML documentation in **core-api/build/docs/javadoc** directory
* Browse **core-api/build/docs/index.html** in browser.

### Manual
		
* Open project in IntelliJ.
* Select **core-api** from projects side bar.
* Go to **Tools -> Generate JavaDoc**
* Uncheck **Include test sources**.
* Select **Output Directory**.
* Click **OK**.
* This will generate HTML documentation in given **Output Directory**.
* Browse **Output Directory/index.html** in browser.
