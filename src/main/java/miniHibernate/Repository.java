package miniHibernate;


import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Repository {

    public static void createTable(String tableName, List<String> fields, List<String> types) throws SQLException {
        Connection connection = new ConexaoBanco().conection();

        List<String> newTypes = changeType(types);
        String datatypes = "";
        int index = 0;
        while (index < fields.size()){
            if(index == fields.size() - 1){
                datatypes += fields.get(index) + " " + newTypes.get(index);
            }else{
                datatypes += fields.get(index) + " " + newTypes.get(index) + ", ";
            }
            index ++;
        }
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName.toLowerCase() + "( " + datatypes + ");";

        try {
            connection.createStatement().executeUpdate(sql);
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connection.close();
        }
    }

    public static void insert(String classPath, Object object) throws ClassNotFoundException, IllegalAccessException, SQLException {
        Connection connection = new ConexaoBanco().conection();

        List<String> fields = ReflactionOperations.getFields(classPath);
        List<String> types = ReflactionOperations.getTypes(classPath);
        String tableName = ReflactionOperations.getName(classPath).toLowerCase();
        Field[] declaredFields = ReflactionOperations.getDeclaredFields(classPath);

        StringBuilder insertStatement = new StringBuilder();
        insertStatement.append("INSERT INTO ").append(tableName).append("( ");
        int index = 0;

        for(String field : fields){
            if(index == fields.size() - 1){
                insertStatement.append(field + " ) VALUES(");
            }else{
                insertStatement.append(field + ",");
            }
            index++;
        }

        index = 0;
        for(Field declaredField : declaredFields){
            declaredField.setAccessible(true);

            if(index == fields.size() - 1) {
                if (declaredField.getName().equals(fields.get(index))) {
                    if (declaredField.get(object) != null) {
                        if (types.get(index).equals("String")) {
                            insertStatement.append('\'').append(declaredField.get(object)).append("');");
                        } else {
                            insertStatement.append(declaredField.get(object).toString()).append(");");
                        }
                    }
                }
            }else{
                if (declaredField.getName().equals(fields.get(index))) {
                    if (declaredField.get(object) != null) {
                        if (types.get(index).equals("String")) {
                            insertStatement.append('\'').append(declaredField.get(object)).append('\'').append(", ");
                        } else {
                            insertStatement.append(declaredField.get(object).toString()).append(", ");
                        }
                    }
                }
            }
            index++;
        }
        System.out.println(insertStatement);

        connection.createStatement().execute(insertStatement.toString());
    }

    public static List<Dataset> getAll(String classPath){
        try{
            String tableName = ReflactionOperations.getName(classPath);
            String sql = "SELECT * FROM " + tableName;
            return(fetch(classPath, sql));
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Dataset> fetch(String classPath, String sql) throws SQLException, ClassNotFoundException {
        Connection connection = new ConexaoBanco().conection();

        ResultSet rs = connection.createStatement().executeQuery(sql);

        List<Dataset> rsList = new ArrayList<>();
        List<String> fields = ReflactionOperations.getFields(classPath);

        while(rs.next()){
            for(String field : fields){
                rsList.add(new Dataset(field, rs.getObject(field).toString()));
            }
        }
        connection.close();

        String rowPrint = "";

        int i = 0;
        for(Dataset dataset : rsList){
            rowPrint += dataset.getField() + ": " + dataset.getValue() + " | ";
            if((i + 1) % fields.size() == 0){
                System.out.println(rowPrint);
                System.out.println("------");
                rowPrint = "";
            }
            i++;
        }

        return rsList;
    }

    public static void deleteAt(String tableName, String column, String key){
        Connection connection = new ConexaoBanco().conection();

        try{
            String sql = "DELETE FROM " + tableName + " WHERE " + column + " = '" + key + "';";
            connection.createStatement().executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Dataset> findByKey(String classPath, String column, String key){
        try{
            String tableName = ReflactionOperations.getName(classPath);
            String sql = "SELECT * FROM " + tableName + " WHERE " + column + " = '" + key + "';";
            System.out.println(sql);
            return(fetch(classPath, sql));
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private static List<String> changeType(List<String> types){
        List<String> newTypes = new ArrayList<>();
        for(String type : types){
            if (type.equals("String")){
                String string = "varchar(255)";
                newTypes.add(string);
            }else{
                newTypes.add(type);
            }
        }
        return newTypes;
    }

}
