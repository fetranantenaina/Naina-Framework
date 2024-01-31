import java.io.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Generation {
    private String base;
    private String tableName;
    private String className;
    private String packageName;
    private String language;
    private JsonObject configuration;
    private List<String> imports = new ArrayList<>();
    private List<String> fields = new ArrayList<>();
    private String type;
    private String primaryKey;

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet resultSet = metaData.getPrimaryKeys(null,null,getTableName());
        JsonObject jsonObject = getConfiguration().getAsJsonObject(getLanguage().toLowerCase());
        jsonObject = jsonObject.getAsJsonObject(getBase().toLowerCase());
        String pK = "";
        while (resultSet.next()){
            String columnName = resultSet.getString("COLUMN_NAME");
            pK = columnName;
        }
        primaryKey = pK;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
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

    public JsonObject getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonObject configuration) {
        this.configuration = configuration;
    }

    public void setConfiguration() throws Exception{
        Reader reader = new FileReader("./config.json");
        Gson gson = new Gson();
        setConfiguration(gson.fromJson(reader, JsonObject.class));
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public Generation(String tableName, String className, String packageName, String language, String base, String type) throws Exception {
        setBase(base);
        setTableName(tableName);
        setClassName(className);
        setPackageName(packageName.replace('.', '/'));
        setLanguage(language);
        setType(type);
        setConfiguration();
        Connection co = connect();
        setPrimaryKey(co);
        setFields(co);
    }

    public Connection connect() throws Exception{
        JsonObject connectionString = getConfiguration().getAsJsonObject("connection");
        String url = connectionString.getAsJsonPrimitive("url").getAsString();
        String user = connectionString.getAsJsonPrimitive("user").getAsString();
        String password = connectionString.getAsJsonPrimitive("password").getAsString();
        Connection connection = DriverManager.getConnection(url, user, password);
        return connection;
    }

    public void createPackageDir() throws Exception{
        String targetLink = getConfiguration().getAsJsonObject("config").getAsJsonPrimitive("target").getAsString();
        String source = getConfiguration().getAsJsonObject("config").getAsJsonPrimitive("source").getAsString();
        String fileLink = targetLink + "\\" + source + "\\" + getPackageName();
        if (getType().equalsIgnoreCase("view")){
            targetLink = getConfiguration().getAsJsonObject("config").getAsJsonPrimitive("targetView").getAsString();
            fileLink = targetLink + "\\" + getPackageName();
        } 
        File packageDir = new File(fileLink);
        if (!packageDir.exists()) {
            if (!packageDir.mkdirs()) {
                throw new Exception("Failed to create package directory.");
            }
        }
    }

    public void createFile() throws Exception{
        String extension = (getLanguage().equals("java"))?"java":"cs";
        if(getLanguage().equalsIgnoreCase("java") && getType().equalsIgnoreCase("view")) extension = "jsp";
        JsonObject choiceTemplate = getConfiguration().getAsJsonObject("choice").getAsJsonObject(getType().toLowerCase());
        String filename = choiceTemplate.getAsJsonPrimitive("filename").getAsString();
        String template = choiceTemplate.getAsJsonPrimitive("file").getAsString();
        String targetLink = getConfiguration().getAsJsonObject("config").getAsJsonPrimitive("target").getAsString();
        String source = getConfiguration().getAsJsonObject("config").getAsJsonPrimitive("source").getAsString();
        String file = getClassName();
        if(getType().equalsIgnoreCase("view")) {
            file = minimalize(getClassName());
            targetLink = getConfiguration().getAsJsonObject("config").getAsJsonPrimitive("targetView").getAsString();
            filename = targetLink + "/" + getPackageName() + "/" + file + "." + extension;
        }else {
            filename = targetLink + "/" + source + "/" + getPackageName() + "/" + file + filename + "." + extension;
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        BufferedReader reader = new BufferedReader(new FileReader("./Template/" + template));
        String line = null;
        while((line = reader.readLine()) != null){
            formatLine(line,writer);
            writer.newLine();
        }
        writer.flush();
    }



    public void formatLine(String line,BufferedWriter writer) throws Exception{
        JsonObject jsonObjectTemplate = getConfiguration().getAsJsonObject(getLanguage().toLowerCase());
        jsonObjectTemplate = jsonObjectTemplate.getAsJsonObject("template");
        if (line.contains("{jvs}") && getLanguage().equalsIgnoreCase("dotnet")) {
            return;
        } else if (line.contains("{dts}") && getLanguage().equalsIgnoreCase("java")) {
            return;
        }
        line = line.replace("{jvs}","").replace("{dts}","");
        if(line.contains("annotation>")) line = getAnnotationLines(writer,jsonObjectTemplate,line);
        if(line.contains("#nom_parameter")) line = line.replace("#nom_parameter",minimalize(getClassName()));
        if(line.contains("#type_id")) line = line.replace("#type_id",getTypeId());
        if(line.contains("#nom_classe")) line = line.replace("#nom_classe",getClassName());
        if(line.contains("<package>")) getPackageLine(line,jsonObjectTemplate,writer);
        else if(line.contains("<import>")) getImportsLine(writer);
        else if(line.contains("<Attributs>")) getFieldsLine(writer);
        else if(line.contains("<getters && setters>")) getGetSet(writer);
        else if(line.contains("<td>{Attributs}</td>")) getTdFieldsLine(writer);
        else if (line.contains("<td>{PrintDataAttributs}</td>")) getTdFieldsData(writer);
        else if (line.contains("<div>{FormAttributs}</div>")) getFormFields(writer);
        else if (line.contains("#nom_PK") || line.contains("#maj_nom_PK")) getPrimaryKeyForm(line,writer);
        else writer.write(line);
    }

    public void getPrimaryKeyForm(String line,BufferedWriter writer) throws IOException{
        if(getPrimaryKey() == "") line = "";
        else{
            if(line.contains("#nom_PK")) line = line.replace("#nom_PK",getPrimaryKey());
            if(line.contains("#maj_nom_PK")) line = line.replace("#maj_nom_PK",capitalize(getPrimaryKey()));
        }
        writer.write(line);
    }


    public String getTypeId(){
        for (String field:getFields()) {
            if (getPrimaryKey().equals(field.split(" ")[1].replace(";", ""))) {
                return field.split(" ")[0];
            }
        }
        return "Integer";
    }


    public void getPackageLine(String line,JsonObject template,BufferedWriter writer) throws IOException {
        String source = getConfiguration().getAsJsonObject("config").getAsJsonPrimitive("source").getAsString();
        if(getType().equalsIgnoreCase("view")) {
            line = line.replace("<package>",source);
        }else {
            line = line.replace("<package>",template.getAsJsonPrimitive("package").getAsString());
            line = line.replace("#nom_package",source + "." + getPackageName());
        }
        writer.write(line);
    }

    public void getImportsLine(BufferedWriter writer) throws IOException {
        if(getType().equalsIgnoreCase("restcontroller")) setImportForController("importRestController");
        else if (getType().equalsIgnoreCase("controller")) setImportForController("importController");
        for (String _import : getImports()) {
            if(!_import.equals("")){
                writer.write(_import);
                writer.newLine();
            }
        }
    }

    public void setImportForController(String typeController){
        JsonObject jsonObjectTemplate = getConfiguration().getAsJsonObject(getLanguage().toLowerCase());
        jsonObjectTemplate = jsonObjectTemplate.getAsJsonObject("template");
        JsonArray imports = jsonObjectTemplate.getAsJsonArray(typeController);
        for (JsonElement each : imports) {
            getImports().add(each.getAsString());
        }
    }

    public void getFieldsLine(BufferedWriter writer) throws IOException {
        for (String field : getFields()) {
            if (!field.equals("")) {
                if (getType().equalsIgnoreCase("entity") && getPrimaryKey().equals(field.split(" ")[1].replace(";", ""))) {
                    if (getLanguage().equalsIgnoreCase("java")) writer.write("\t@Id");
                    else writer.write("\t[Key]");
                    writer.newLine();
                }
                if (getType().equalsIgnoreCase("entity") && field.split(" ")[0].equals("Date")) {
                    writer.write("\t@DateTimeFormat(pattern = \"yyyy-MM-dd\")");
                    writer.newLine();
                }
                writer.write("\t" + field);
                writer.newLine();
            }
        }
    }

    public void getTdFieldsLine(BufferedWriter writer) throws IOException {
        for (String field : getFields()) {
            if(!field.equals("")){
                writer.write("\t\t\t\t\t<td>" + capitalize(field.split(" ")[1].replace(";","")) + "</td>");
                writer.newLine();
            }
        }
    }

    public void getTdFieldsData(BufferedWriter writer) throws IOException {
        for (String field : getFields()) {
            if(!field.equals("")){
                writer.write("\t\t\t\t\t\t<td><% out.print("+ minimalize(getClassName()) + ".get" + capitalize(field.split(" ")[1].replace(";","")) + "()); %></td>");
                writer.newLine();
            }
        }
    }

    public void getFormFields(BufferedWriter writer) throws IOException{
        for (String field : getFields()) {
            if(!field.equals("")){
                String nomField = field.split(" ")[1].replace(";","");
                writer.write("\t<div>\n");
                if(!nomField.equalsIgnoreCase(getPrimaryKey()))
                    writer.write("\t\t<label for=\""+ minimalize(nomField) +"\">"+ capitalize(nomField) +"</label>\n");
                writer.write(fieldForm(nomField));
                writer.write("\t</div>\n");
            }
        }
    }

    public String fieldForm(String nomField){
        if(nomField.equalsIgnoreCase(getPrimaryKey())){
            return "\t\t<input type=\"hidden\" name=\""+ minimalize(nomField) +"\" id=\""+minimalize(nomField.replace(";",""))+"\" class=\"form-control\">\n";
        } else if((!nomField.equalsIgnoreCase(getPrimaryKey())) && nomField.contains("id")){
            String objet = nomField.split("_")[1];
            return "        <select class=\"form-select my-2\" name=\""+ minimalize(nomField) +"\">\n" +
                    "            <option value=\"\">Choisissez le "+ objet +"</option>\n" +
                    "            <% for("+capitalize(objet)+" " + objet + " : "+objet+"s) { %>\n" +
                    "                <option value=\"<% out.println("+objet+".getId_"+objet+"()); %>\"><% out.println("+objet+".getNom_"+objet+"()); %></option>\n" +
                    "            <% } %>\n" +
                    "        </select>";
        }else return "\t\t<input type=\"text\" name=\""+ minimalize(nomField) +"\" id=\""+minimalize(nomField.replace(";",""))+"\" class=\"form-control\">\n";
    }

    public String getAnnotationLines(BufferedWriter writer,JsonObject template,String line) throws IOException {
        if(line.contains("<controller_annotation>")) getAnnotationLineFor(writer,template,"controller_annotation");
        else if(line.contains("<class_annotation>")) getAnnotationLineFor(writer,template,"class_annotation");
        else if(line.contains("<restcontroller_annotation>")) getAnnotationLineFor(writer,template,"restcontroller_annotation");
        else {
            String annotation = line.split("<")[1].split(">")[0];
            line = line.replace("<" + annotation + ">",template.getAsJsonPrimitive(annotation).getAsString());
            return line;
        }
        return "";
    }

    public void getAnnotationLineFor(BufferedWriter writer,JsonObject template,String array) throws IOException {
        JsonArray annots = template.getAsJsonArray(array);
        for (JsonElement each:annots) {
            String annotation = each.getAsString();
            if(annotation.contains("#nom_classe")) annotation = annotation.replace("#nom_classe",getClassName());
            if(annotation.contains("#nom_parameter")) annotation = annotation.replace("#nom_parameter",minimalize(getClassName()));
            writer.write(annotation);
            writer.newLine();
        }
    }

    private String capitalize(String input) {
        String first = input.charAt(0) + "";
        return input.replaceFirst(first,first.toUpperCase());
    }

    private String minimalize(String input) {
        String first = input.charAt(0) + "";
        return input.replaceFirst(first,first.toLowerCase());
    }

    public void getGetSet(BufferedWriter writer) throws IOException {
        for (String field : getFields()) {
            String type = field.split(" ")[0];
            String variable = field.split(" ")[1].replace(";","");
            if(!field.equals("")){
                writer.write("\tpublic " + type + " get" + capitalize(variable) + "() {\n");
                writer.write("\t\treturn this." + variable + ";\n");
                writer.write("\t}\n\n");
                writer.write("\tpublic void set" + capitalize(variable) + "(" + type + " " + variable + ") {\n");
                writer.write("\t\tthis." + variable + " = " + variable + ";\n");
                writer.write("\t}\n\n");
            }
        }
    }

    public void setFields(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet resultSet = metaData.getColumns(null, null, getTableName(), null);
        JsonObject jsonObject = getConfiguration().getAsJsonObject(getLanguage().toLowerCase());
        jsonObject = jsonObject.getAsJsonObject(getBase().toLowerCase());
        while (resultSet.next()){
            String columnName = resultSet.getString("COLUMN_NAME");
            String dataType = resultSet.getString("TYPE_NAME");
            JsonObject fieldObject = jsonObject.getAsJsonObject(dataType);
            String type = fieldObject.getAsJsonPrimitive("type").getAsString();
            String importS = fieldObject.getAsJsonPrimitive("import").getAsString();
            fields.add(type + " " + columnName + ";");
            imports.add(importS);
        }
    }

    public void generate() throws Exception{
        createPackageDir();
        createFile();
    }


}
