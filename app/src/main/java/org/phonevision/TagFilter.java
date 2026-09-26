package org.phonevision;

import java.util.HashSet;
import java.util.Set;

/** ID allow / block lists. Text format: "1, 3-6, 12" (commas or spaces, ranges allowed). */
public final class TagFilter {
    private TagFilter() {}

    public static Set<Integer> parse(String text) {
        Set<Integer> out = new HashSet<>();
        if (text == null) return out;
        for (String tok : text.split("[,;\\s]+")) {
            if (tok.isEmpty()) continue;
            try {
                int dash = tok.indexOf('-', 1);
                if (dash > 0) {
                    int a = Integer.parseInt(tok.substring(0, dash)), b = Integer.parseInt(tok.substring(dash + 1));
                    if (a > b) { int t = a; a = b; b = t; }
                    for (int i = a; i <= b && i - a < 1000; i++) out.add(i);
                } else out.add(Integer.parseInt(tok));
            } catch (NumberFormatException ignored) { /* skip bad token */ }
        }
        return out;
    }

    /** Block list always wins; an empty allow list means "all IDs". */
    public static boolean accepts(Set<Integer> allow, Set<Integer> block, int id) {
        if (block.contains(id)) return false;
        return allow.isEmpty() || allow.contains(id);
    }
}
