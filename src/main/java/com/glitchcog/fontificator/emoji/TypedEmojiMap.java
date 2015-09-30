package com.glitchcog.fontificator.emoji;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.glitchcog.fontificator.config.ConfigEmoji;

/**
 * Internally contains two maps, one for storing emoji that are keyed off regular expressions, and another for storing
 * emoji that are keyed off just a word. This separation is to permit the speedier access of emoji keyed off of words
 * before checking through all the regular expression keys, which take longer
 * 
 * @author Matt Yanos
 */
public class TypedEmojiMap
{
    private final EmojiType type;

    private Map<String, LazyLoadEmoji[]> normalMap;

    private Map<String, LazyLoadEmoji[]> regexMap;

    public TypedEmojiMap(EmojiType type)
    {
        this.type = type;
        normalMap = new HashMap<String, LazyLoadEmoji[]>();
        regexMap = new HashMap<String, LazyLoadEmoji[]>();
    }

    /**
     * Get the emoji
     * 
     * @param testKey
     * @return emoji or null if not found
     */
    public LazyLoadEmoji[] getEmoji(String testKey)
    {
        return getEmoji(testKey, null);
    }

    public LazyLoadEmoji[] put(String key, LazyLoadEmoji[] value)
    {
        if (isRegularExpression(key))
        {
            key = fixRegularExpression(key);
            return regexMap.put(key, value);
        }
        else
        {
            return normalMap.put(key, value);
        }
    }

    /**
     * Get the emoji, if the configuration allows for that type of emoji
     * 
     * @param testKey
     * @param config
     * @return emoji or null if not found or if configuration prohibits this emoji
     */
    public LazyLoadEmoji[] getEmoji(String testKey, ConfigEmoji config)
    {
        if (config != null && !config.isTypeEnabledAndLoaded(type))
        {
            return null;
        }

        LazyLoadEmoji[] emoji = normalMap.get(testKey);

        if (emoji == null)
        {
            for (String regex : regexMap.keySet())
            {
                if (testKey.matches(regex))
                {
                    emoji = regexMap.get(regex);
                    break;
                }
            }
        }

        final boolean barredByConfigSubscriberDisabled = config != null && config.isTwitchSubscriberDisable() && (emoji != null && emoji.length > 0 && emoji[0].getType().isTwitch() && emoji[0].isSubscriber());

        return barredByConfigSubscriberDisabled ? null : emoji;
    }

    public Collection<String> keySet()
    {
        Set<String> keys = new HashSet<String>(normalMap.keySet().size() + regexMap.keySet().size());
        keys.addAll(normalMap.keySet());
        keys.addAll(regexMap.keySet());
        return keys;
    }

    /**
     * Determine whether the specified key is a regular expression or just a word, based on whether it is comprised
     * entirely of alpha numeric characters indicating it's just a word
     * 
     * @param key
     * @return is a regular expression
     */
    private static boolean isRegularExpression(String key)
    {
        return !key.matches("^[a-zA-Z0-9_]*$");
    }

    /**
     * Remove extraneous escape characters and decode xml entities. This may need to be tinkered with.
     * 
     * @param regex
     * @return fixed regex
     */
    private static String fixRegularExpression(String regex)
    {
        // Remove redundant slashes
        regex = regex.replaceAll("\\\\\\\\", "\\\\");
        // Un-escape semicolons
        regex = regex.replaceAll("\\\\;", ";");
        regex = regex.replaceAll("\\\\:", ":");
        regex = regex.replaceAll("\\\\&", "&");
        // Greater than and less than xml entities
        regex = regex.replaceAll("&lt;", "<");
        regex = regex.replaceAll("&gt;", ">");
        return regex;
    }
}