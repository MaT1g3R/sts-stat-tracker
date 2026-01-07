package StatsTracker;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    private static final Map<String, String> localizedCache = new HashMap<>();

    public static String getLocalizedName(Object obj) {
        if (obj == null) return "";
        if (!(obj instanceof String)) return obj.toString();
        String s = (String) obj;

        if (localizedCache.containsKey(s)) {
            return localizedCache.get(s);
        }

        String res = findCardName(s);
        if (isValid(res)) return cache(s, res);

        res = findRelicName(s);
        if (isValid(res)) return cache(s, res);

        res = findEventName(s);
        if (isValid(res)) return cache(s, res);

        res = findEncounterName(s);
        if (isValid(res)) return cache(s, res);

        res = findMonsterName(s);
        if (isValid(res)) return cache(s, res);

        return cache(s, s);
    }

    private static boolean isValid(String s) {
        return s != null && !s.contains("MISSING_NAME");
    }

    private static String cache(String key, String value) {
        localizedCache.put(key, value);
        return value;
    }

    private static String findCardName(String id) {
        try {
            AbstractCard c = com.megacrit.cardcrawl.helpers.CardLibrary.getCard(id);
            if (c != null) return c.name;
        } catch (Exception e) {}
        return null;
    }

    private static String findRelicName(String id) {
        try {
            AbstractRelic r = RelicLibrary.getRelic(id);
            if (isValidRelic(r, id)) return r.name;

            r = RelicLibrary.getRelic(id.replace(" ", ""));
            if (isValidRelic(r, id)) return r.name;
        } catch (Exception e) {}
        return null;
    }

    private static boolean isValidRelic(AbstractRelic r, String id) {
        return r != null && (!r.relicId.equals("Circlet") || id.equals("Circlet"));
    }

    private static String findEventName(String id) {
        try {
            com.megacrit.cardcrawl.localization.EventStrings es = com.megacrit.cardcrawl.core.CardCrawlGame.languagePack.getEventString(id);
            if (es != null && es.NAME != null) return es.NAME;
        } catch (Exception e) {}
        return null;
    }

    private static String findEncounterName(String id) {
        try {
            // Normalize legacy IDs to standard Game IDs
            String normalizedId = normalizeEncounterId(id);
            String name = com.megacrit.cardcrawl.helpers.MonsterHelper.getEncounterName(normalizedId);
            if (name != null && !name.equals("[MISSING_NAME]")) {
                return name;
            }
        } catch (Exception e) {}
        return null;
    }

    private static String findMonsterName(String id) {
        try {
            com.megacrit.cardcrawl.localization.MonsterStrings ms = com.megacrit.cardcrawl.core.CardCrawlGame.languagePack.getMonsterStrings(id);
            if (ms != null && ms.NAME != null) return ms.NAME;
        } catch (Exception e) {}
        return null;
    }

    public static String normalizeEncounterId(String id) {
        switch (id) {
            case "BookOfStabbing": return "Book of Stabbing";
            case "HealerTank": return "Centurion and Healer";
            case "3_Byrds": return "3 Byrds";
            case "GiantHead": return "Giant Head";
            case "Darkling Encounter": return "3 Darklings";
            case "Gremlin Leader Combat": return "Gremlin Leader";
            case "SlimeBoss": return "Slime Boss";
            case "Double Orb Walker": return "2 Orb Walkers";
            case "JawWorm": return "Jaw Worm";
            case "Shelled Parasite": return "Shell Parasite";
            case "GremlinNob": return "Gremlin Nob";
            case "Sentries": return "3 Sentries";
            default: return id;
        }
    }

    public static void invoke(Object obj, Class<?> clz, String m, Object... args) {
        Method method = null;
        try {
            method = clz.getDeclaredMethod(m);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        method.setAccessible(true);
        try {
            method.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static <A> A getField(Object obj, Class<?> clz, String name) {
        Field field = null;
        A result = null;
        try {
            field = clz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true);
        try {
            result = (A) field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static <A> void setField(Object obj, Class<?> clz, String name, A value) {
        Field field = null;
        try {
            field = obj.getClass().getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true);
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static double calculatePercent(int value, int total) {
        if (total == 0) {
            return 0;
        }
        double rate = (100D * value) / total;
        return round(rate, 3);
    }

    public static double round(double value, int places) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.round(new MathContext(places));
        return bd.doubleValue();
    }

    public static void cardUpgradeName(AbstractCard card) {
        invoke(card, AbstractCard.class, "upgradeName");
    }

    public static void cardCreateCardImage(AbstractCard card) {
        invoke(card, AbstractCard.class, "createCardImage");
    }

    public static void cardUpgradeBlock(AbstractCard card, int amount) {
        card.baseBlock += amount;
        card.upgradedBlock = true;
    }

    public static void cardUpgradeDamage(AbstractCard card, int amount) {
        card.baseDamage += amount;
        card.upgradedDamage = true;
    }

    public static void cardUpgradeMagicNumber(AbstractCard card, int amount) {
        card.baseMagicNumber += amount;
        card.magicNumber = card.baseMagicNumber;
        card.upgradedMagicNumber = true;
    }

}
