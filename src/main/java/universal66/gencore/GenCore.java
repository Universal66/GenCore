package universal66.gencore;

import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class GenCore extends JavaPlugin {
    private static final Logger LOGGER = PluginLogger.getLogger("GenCore");
    private Runnable updatePending = null;

    @Override
    public void onLoad() {
        try {
            var website = URI.create("https://github.com/Universal66/GenCore/releases/latest/download/GenCore.jar").toURL();
            var rbc = Channels.newChannel(website.openStream());
            var fos = new FileOutputStream("plugins/GenCore-update.jar");

            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.getChannel().close();
            fos.close();

            rbc.close();

            var path = Path.of("plugins/GenCore-update.jar");
            var classLoader = new URLClassLoader(new URL[]{
                    path.toUri().toURL()
            });

            var newVersion = getPropertyFromYAML(classLoader.getResourceAsStream("plugin.yml"), "version");
            var oldVersion = "1.0";

            if (newVersion.equalsIgnoreCase(oldVersion)) {
                classLoader.close();
                Files.delete(path);
                LOGGER.warning("No new update available.");
            } else {
                Path self = findThis();
                if (self == null) {
                    LOGGER.severe("Self plugin JAR couldn't be found!");
                    return;
                }

                try {
                    Class<?> cls = classLoader.loadClass("universal66.gencore.UpdateDynamic");
                    var method = cls.getDeclaredMethod("any");
                    method.invoke(null);
                } catch (Exception e) {
//                    LOGGER.warning("Failed to execute on-update code.");
                }

                classLoader.close();

                this.updatePending = () -> {
                    try {
                        while (true) {
                            try {
                                Files.delete(self);
                            } catch (IOException e) {
                                try {
                                    Thread.sleep(70);
                                } catch (InterruptedException ex) {
                                    LOGGER.severe("Update interrupted!");
                                    return;
                                }

                                continue;
                            } catch (Exception e) {
                                LOGGER.severe("Update interrupted!");
                                return;
                            }

                            break;
                        }

                        Files.move(path, self);

                        LOGGER.info("Update successful.");
                    } catch (IOException e) {
                        LOGGER.severe("Failed to update!");
                    }
                };

                LOGGER.info("Update pending.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.severe("Cannot auto-update!");
        }
    }

    private static Path findThis() {
        try (var filesStream = Files.list(Path.of("plugins"))) {
            var files = filesStream.toList();
            for (Path path : files) {
                if (path.getFileName().toString().endsWith(".jar")) {
                    try (
                            URLClassLoader classLoader =
                            new URLClassLoader(new URL[]{path.toUri().toURL()})
                    ) {
                        var name = getPropertyFromYAML(classLoader.getResourceAsStream("plugin.yml"), "name");
                        if (name.equalsIgnoreCase("GenCore")) {
                            return path;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to find the self plugin JAR");
        }

        return null;
    }

    private static String getPropertyFromYAML(InputStream stream, String prop) throws IOException {
        try (stream) {
            var bytes = stream.readAllBytes();
            var lines = new String(bytes).split("\n");

            for (var line : lines) {
                if (line.startsWith(prop + ": ")) {
                    return line.split(":")[1].trim().replaceAll("['\"]", "");
                }
            }
        }

        return "";
    }

    @Override
    public void onDisable() {
        if (updatePending != null)
            new Thread(updatePending).start();
    }
}
