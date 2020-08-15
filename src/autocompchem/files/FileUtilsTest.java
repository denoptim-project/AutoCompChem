package autocompchem.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;


/**
 * Unit Test for file utils.
 * 
 * @author Marco Foscato
 */

public class FileUtilsTest 
{
    @Test
    public void testGetExtension() throws Exception
    {
        File f = new File("./tmp/__ tmp_acc_junit. srtg.tre.ext");
        assertEquals(".ext",FileUtils.getFileExtension(f),
        		"Extention of filename with many dots");
        
        f = new File("/tmp/ __tmp_acc_junit.ext");
        assertEquals(".ext",FileUtils.getFileExtension(f),
        		"Extention of filename with one dot");
        
        f = new File("/tmp/__tmp_acc_junitext");
        assertEquals(null,FileUtils.getFileExtension(f),
        		"Extention of filename with no dot");
    }

}
