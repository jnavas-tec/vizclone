# Development IDE

For development and plugin testing, we used:

```
IntelliJ IDEA 2023.2.1 (Ultimate Edition)
Build #IU-232.9559.62, built on August 22, 2023
Runtime version: 17.0.8+7-b1000.8 x86_64
VM: OpenJDK 64-Bit Server VM by JetBrains s.r.o.
macOS 13.6.4GC: G1 Young Generation, G1 Old Generation
Memory: 2200M
Cores: 8
Registry:
    ide.experimental.ui=true
Non-Bundled Plugins:
    com.intellij.javafx (1.0.4)
    Seanke.CleanDarkTheme (1.0.2)
Kotlin: 232-1.9.0-IJ9559.62
```

# Running the plugin

From the Gradle Toolwindow, select `Run Configurations`/`Run Plugin`
and execute it with `Run` or double-click it.

When the new IntelliJ IDEA instance is running, open the project to
be analyzed. Then from the `Tools` menu select the option `Search Clones`.
