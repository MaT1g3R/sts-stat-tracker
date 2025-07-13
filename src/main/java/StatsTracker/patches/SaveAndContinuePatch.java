//package StatsTracker.patches;
//
//
//import com.evacipated.cardcrawl.modthespire.lib.*;
//import com.google.gson.Gson;
//import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
//import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
//import javassist.CtBehavior;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//@SpirePatch(
//        clz = SaveAndContinue.class,
//        method = "save",
//        paramtypez = {SaveFile.class}
//)
//public class SaveAndContinuePatch {
//    public static final Logger logger = LogManager.getLogger(SaveAndContinuePatch.class.getName());
//
//    private static int count = 0;
//    private static final String dir = "/home/umi/tmp/autosaves";
//
//    @SpireInsertPatch(
//            locator = Locator.class,
//            localvars = {"data"}
//    )
//    public static void Insert(SaveFile save, String data) {
//        logger.info("saving");
//        String path = String.format("%d.json", count);
//        count++;
//        try {
//            Files.write(Paths.get(dir + "/" + path), data.getBytes());
//        } catch (IOException e) {
//            logger.error(e);
//        }
//    }
//
//    private static class Locator extends SpireInsertLocator {
//        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
//            Matcher finalMatcher = new Matcher.MethodCallMatcher(Gson.class, "toJson");
//            int[] result = LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
//            for (int i = 0; i < result.length; i++) {
//                result[i] = result[i] + 1;
//            }
//            return result;
//        }
//    }
//}
