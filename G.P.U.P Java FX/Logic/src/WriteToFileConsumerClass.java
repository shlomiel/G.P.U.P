import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

public class WriteToFileConsumerClass implements Consumer {
    private String path;


    public WriteToFileConsumerClass(String filePath){
        path = "\\"+filePath+".log";
        File newFile = new File(path);

    }

    @Override
    public void accept(Object o) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, true));
            writer.append(o.toString() + "\n");
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }



    }


}
