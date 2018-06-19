# Generate Code Documentation
### Steps	
* Checkout **master** branch.
* Open project in **IntelliJ**.
* Go to **Tools -> Generate JavaDoc**
* Make sure **Generate JavaDoc scope -> Custom Scope** is checked.
* Click on three dotted icon to make custom scope.
* Give name **Custom_Docs**.
* Copy and paste following regex: **`!*..internal..*&&!test:*..*&&!lib:*..*`** into **Pattern** and click **OK**.
* Uncheck **Include test sources**.
* Select **Output Directory**.
* Click **OK**.
* This will generate HTML documentation in given **Output Directory**.
* Browse **Output Directory/index.html** in browser.
