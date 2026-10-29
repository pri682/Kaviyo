package com.kaviyo.crawler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Small RFC 9309-oriented parser for user-agent groups and path-prefix allow/disallow rules. */
final class RobotsRules {
    private RobotsRules() {
    }

    static boolean allows(String body, URI target, String userAgent) {
        List<Group> groups = parse(body);
        List<Rule> specificRules = new ArrayList<>();
        List<Rule> wildcardRules = new ArrayList<>();
        for (Group group : groups) {
            boolean specific = group.agents().stream().anyMatch(agent ->
                    !agent.equals("*") && userAgent.toLowerCase(Locale.ROOT).contains(agent));
            boolean wildcard = group.agents().contains("*");
            if (specific) {
                specificRules.addAll(group.rules());
            } else if (wildcard) {
                wildcardRules.addAll(group.rules());
            }
        }
        List<Rule> rules = specificRules.isEmpty() ? wildcardRules : specificRules;
        String path = target.getRawPath() == null || target.getRawPath().isBlank()
                ? "/" : target.getRawPath();
        final String targetPath = target.getRawQuery() == null ? path : path + "?" + target.getRawQuery();
        Rule match = rules.stream()
                .filter(rule -> !rule.path().isEmpty() && rule.matches(targetPath))
                .max((left, right) -> {
                    int length = Integer.compare(left.specificity(), right.specificity());
                    if (length != 0) {
                        return length;
                    }
                    return Boolean.compare(left.allow(), right.allow());
                })
                .orElse(null);
        return match == null || match.allow();
    }

    private static List<Group> parse(String body) {
        List<Group> groups = new ArrayList<>();
        List<String> agents = new ArrayList<>();
        List<Rule> rules = new ArrayList<>();
        boolean sawRule = false;
        for (String rawLine : body.split("\\R")) {
            String line = rawLine.split("#", 2)[0].trim();
            if (line.isEmpty()) {
                if (!agents.isEmpty() && sawRule) {
                    groups.add(new Group(List.copyOf(agents), List.copyOf(rules)));
                    agents.clear();
                    rules.clear();
                    sawRule = false;
                }
                continue;
            }
            int separator = line.indexOf(':');
            if (separator < 0) {
                continue;
            }
            String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(separator + 1).trim();
            if (key.equals("user-agent")) {
                if (sawRule) {
                    groups.add(new Group(List.copyOf(agents), List.copyOf(rules)));
                    agents.clear();
                    rules.clear();
                    sawRule = false;
                }
                agents.add(value.toLowerCase(Locale.ROOT));
            } else if (!agents.isEmpty() && (key.equals("allow") || key.equals("disallow"))) {
                rules.add(new Rule(key.equals("allow"), value));
                sawRule = true;
            }
        }
        if (!agents.isEmpty()) {
            groups.add(new Group(List.copyOf(agents), List.copyOf(rules)));
        }
        return groups;
    }

    private record Rule(boolean allow, String path) {
        private boolean matches(String targetPath) {
            StringBuilder regex = new StringBuilder("^");
            boolean anchoredAtEnd = path.endsWith("$");
            int end = anchoredAtEnd ? path.length() - 1 : path.length();
            for (int i = 0; i < end; i++) {
                char character = path.charAt(i);
                if (character == '*') {
                    regex.append(".*");
                } else {
                    regex.append(Pattern.quote(String.valueOf(character)));
                }
            }
            if (anchoredAtEnd) {
                regex.append('$');
            } else {
                regex.append(".*");
            }
            return Pattern.compile(regex.toString()).matcher(targetPath).matches();
        }

        private int specificity() {
            return (int) path.chars().filter(character -> character != '*' && character != '$').count();
        }
    }

    private record Group(List<String> agents, List<Rule> rules) {
    }
}
