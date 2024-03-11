package StatsTracker;

import com.megacrit.cardcrawl.cards.AbstractCard;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

public class Utils {

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
        BigDecimal bd = new BigDecimal(rate);
        bd = bd.round(new MathContext(3));
        return bd.doubleValue();
    }

    public static String normalizeCardName(String s) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("Lockon", "Bullseye");
        replacements.put("Steam", "Steam Barrier");
        replacements.put("Steam Power", "Overclock");
        replacements.put("Redo", "Recursion");
        replacements.put("Undo", "Equilibrium");
        replacements.put("Gash", "Claw");
        replacements.put("Venomology", "Alchemize");
        replacements.put("Night Terror", "Nightmare");
        replacements.put("NightTerror", "Nightmare");
        replacements.put("Crippling Poison", "Crippling Cloud");
        replacements.put("CripplingPoison", "Crippling Cloud");
        replacements.put("Underhanded Strike", "Sneaky Strike");
        replacements.put("UnderhandedStrike", "Sneaky Strike");
        replacements.put("Clear The Mind", "Tranquility");
        replacements.put("ClearTheMind", "Tranquility");
        replacements.put("Wireheading", "Foresight");
        replacements.put("Vengeance", "Simmering Fury");
        replacements.put("Adaptation", "Rushdown");
        replacements.put("Path to Victory", "Pressure Points");
        replacements.put("PathToVictory", "Pressure Points");
        replacements.put("Ghostly", "Apparition");
        replacements.put("Yang", "Duality");
        replacements.put("Snake Skull", "Snecko Skull");

        String normal = s.split("\\+")[0];
        return replacements.getOrDefault(normal, normal);
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
