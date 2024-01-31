import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.io.File;
import java.io.FileReader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.Reader;

public class Generator {
    private String url;
    private String user;
    private String password;
    private String port;
    private String base;
    private String tableName;
    private String className;
    private String packageName;
    private String language;

    private Gson gson;
    private Reader reader;

    public Generator(String tableName, String className, String packageName, String language, String base)
            throws Exception {
        this.gson = new Gson();
        getConfig();
        this.tableName = tableName;
        this.className = className;
        this.packageName = packageName;
        this.language = language;
        this.base = base;
    }

    public Generator() throws Exception {
        gson = new Gson();
        getConfig();
    }

    public void getConfig() throws Exception {
        try {
            reader = new FileReader("./config.json");
            // Désérialiser le JSON en un objet JsonObject
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            json = json.getAsJsonObject("connection");
            url = json.getAsJsonPrimitive("url").getAsString();
            user = json.getAsJsonPrimitive("user").getAsString();
            password = json.getAsJsonPrimitive("password").getAsString();
            port = json.getAsJsonPrimitive("port").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("erreur de configuration de la connexion");
        }

    }

    private String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public void createPackage() {
        File packageDir = new File(packageName.replace('.', '/'));
        if (!packageDir.exists()) {
            if (packageDir.mkdirs()) {
                System.out.println("Package directory created: " + packageDir.getAbsolutePath());
            } else {
                System.out.println("Failed to create package directory.");
                System.exit(1);
            }
        }
    }

    public void createJavaFile(String classContent) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(packageName.replace('.', '/') + "/" + className + ".java"))) {
            writer.write(classContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String create_attribute(String type, String attributeName) {
        try {
            reader = new FileReader("./config.json");

            // Désérialiser le JSON en un objet JsonObject
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            // Accéder à la section "java"
             json = json.getAsJsonObject(language.toLowerCase());
             json = json.getAsJsonObject("template");

            // Vérifier si la section "java" existe
            if (json != null) {
                // Accéder aux éléments de la section "java"
                String attribute = json.getAsJsonPrimitive("attribute").getAsString();
                attribute = attribute.replace("<type>", type);
                attribute = attribute.replace("<attributeName>", attributeName);

                return attribute;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String create_getter_setter(String type, String attributeName) {
        try {
            reader = new FileReader("./config.json");
            // Désérialiser le JSON en un objet JsonObject
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            // Accéder à la section "java"
             json = json.getAsJsonObject(language.toLowerCase());
             json = json.getAsJsonObject("template");
            // Vérifier si la section "java" existe
            if (json != null) {
                String caps_attributeName = capitalize(attributeName);
                // Accéder aux éléments de la section "java"
                String getter_setter = json.getAsJsonPrimitive("getter_setter").getAsString();
                getter_setter = getter_setter.replace("<type>", type);
                getter_setter = getter_setter.replace("<attributeName>", attributeName);
                getter_setter = getter_setter.replace("<caps_attributeName>", caps_attributeName);

                return getter_setter;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public HashMap<String, StringBuilder> create_Content(Connection connection)
            throws SQLException {
        // Récupérer les métadonnées de la base de données
        HashMap<String, StringBuilder> map = new HashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();
        // Récupérer les colonnes de la table
        ResultSet resultSet = metaData.getColumns(null, null, tableName, null);
        // Générer le code de la classe
        StringBuilder attributesBuilder = new StringBuilder();
        StringBuilder getters_setters_builder = new StringBuilder();
        StringBuilder importsbuilder = new StringBuilder();

        while (resultSet.next()) {
            String columnName = resultSet.getString("COLUMN_NAME");
            String dataType = resultSet.getString("TYPE_NAME");
            System.out.println(dataType.toUpperCase());
            // Générer le code pour chaque colonne
            HashMap<String, String> type_import = getType(dataType);
            String type = type_import.get("type");
            System.out.println(type);
            // String importing = type_import.get("import");
            if (!type_import.get("import").trim().isEmpty()) {
                importsbuilder.append(type_import.get("import").trim());
            }
            // String fieldDeclaration = "\tprivate " + type + " " + columnName + ";\n";
            String atributeDeclaration = create_attribute(type, columnName);
            attributesBuilder.append(atributeDeclaration);
            // conserver les getters et setters dans getters_setters_builder
            getters_setters_builder.append(create_getter_setter(type, columnName));
        }
        map.put("attributes", attributesBuilder);
        map.put("getters_setters", getters_setters_builder);
        map.put("imports", importsbuilder);

        return map;
    }

    public String create_design(Connection connection) {
        try {
            // Désérialiser le JSON en un objet JsonObject
            reader = new FileReader("./config.json");
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            // Accéder à la section "java"
             json = json.getAsJsonObject("java");
             json = json.getAsJsonObject("template");
            // Vérifier si la section "java" existe
            if (json != null) {
                HashMap<String, StringBuilder> map = create_Content(connection);
                // Accéder aux éléments de la section "java"
                String design = json.getAsJsonPrimitive("design").getAsString();
                design = design.replace("<folder>", packageName);
                design = design.replace("<classname>", className);
                design = design.replace("<imports>", map.get("imports").toString());
                design = design.replace("<attributes>", map.get("attributes").toString());
                design = design.replace("<getters_setters>", map.get("getters_setters").toString());

                return design;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private HashMap<String, String> getType(String sqlType) {
        HashMap<String, String> map = new HashMap<>();

        try {
            reader = new FileReader("./config.json");

            // Désérialiser le JSON en un objet JsonObject
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            // Accéder à la section "java"
             json = json.getAsJsonObject(language.toLowerCase());
             json = json.getAsJsonObject(base.toLowerCase());
             json = json.getAsJsonObject(sqlType.toLowerCase());
            String typeName = json.getAsJsonPrimitive("type").getAsString();
            String importing = json.getAsJsonPrimitive("import").getAsString();
            map.put("type", typeName);
            map.put("import", importing);

            return map;
        } catch (Exception e) {
            e.printStackTrace();
            map.put("type", "Object");
            map.put("import", "");

            return map;
        }
    }

    public void generate() {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            String classContent = create_design(connection);
            // Créer le répertoire si nécessaire
            createPackage();
            // Écrire le contenu dans un fichier Java
            createJavaFile(classContent);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    @Override
    public String toString() {
        return "Generator [url=" + url + ", user=" + user + ", password=" + password + ", port=" + port + ", base="
                + base + ", tableName=" + tableName + ", className=" + className + ", packageName=" + packageName
                + ", language=" + language + ", reader=" + reader + "]";
    }

}
