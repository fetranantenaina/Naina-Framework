import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Generate {

    public static void main(String[] args) {
        try{
            //java Generate java class model Exemple postgres exemples
            //language type package classname base tableName
            String language = args[0];
            String type = args[1];
            String packageName = args[2];
            String className = args[3];
            String base = args[4];
            String tableName = args[5];
            if(type.equalsIgnoreCase("mvc")){
                String[] allType = {"entity","repository","service","controller"};
                String[] allPackage = {"Modele","Repository","Service","Controller"};
                for (int i = 0; i < allType.length; i++) {
                    type = allType[i];
                    packageName = allPackage[i];
                    Generation generation = new Generation(tableName, className, packageName, language, base, type);
                    generation.generate();
                }
            }else {
                Generation generation = new Generation(tableName, className, packageName, language, base, type);
                generation.generate();
            }
        } catch (Exception e) {
            System.out.println("Usage: java Generate <language> <Class/Controller> <package> <class_name> <base> <table_name>");
            System.out.println(e.getMessage());
//            System.exit(1);
            e.printStackTrace();
        }
    }
}
