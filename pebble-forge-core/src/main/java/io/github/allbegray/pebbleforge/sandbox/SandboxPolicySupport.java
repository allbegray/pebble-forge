package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Test;
import io.pebbletemplates.pebble.extension.core.DefaultFilter;
import io.pebbletemplates.pebble.extension.core.DefinedTest;
import io.pebbletemplates.pebble.template.PebbleTemplate;

final class SandboxPolicySupport {

    enum Kind {
        TAG("Tag"),
        FILTER("Filter"),
        FUNCTION("Function"),
        TEST("Test"),
        GLOBAL("Global variable");

        private final String label;

        Kind(String label) {
            this.label = label;
        }
    }

    private SandboxPolicySupport() {
    }

    static SandboxPolicy policyFor(SandboxPolicyResolver policies, String templateName) {
        return templateName == null ? policies.defaultPolicy() : policies.resolve(templateName);
    }

    static boolean allowed(SandboxPolicy policy, Kind kind, String name) {
        return switch (kind) {
            case TAG -> policy.isTagAllowed(name);
            case FILTER -> policy.isFilterAllowed(name);
            case FUNCTION -> policy.isFunctionAllowed(name);
            case TEST -> policy.isTestAllowed(name);
            case GLOBAL -> policy.isGlobalVariableAllowed(name);
        };
    }

    static boolean anyAllow(SandboxPolicyResolver policies, Kind kind, String name) {
        for (SandboxPolicy policy : policies.policies()) {
            if (allowed(policy, kind, name)) {
                return true;
            }
        }
        return false;
    }

    static boolean allAllow(SandboxPolicyResolver policies, Kind kind, String name) {
        for (SandboxPolicy policy : policies.policies()) {
            if (!allowed(policy, kind, name)) {
                return false;
            }
        }
        return true;
    }

    static void requireAllowed(SandboxPolicyResolver policies, PebbleTemplate self,
            Kind kind, String name, int lineNumber) {
        String owner = PolicyContext.current();
        String templateName = owner == null ? self.getName() : owner;
        SandboxPolicy policy = policyFor(policies, templateName);
        if (!allowed(policy, kind, name)) {
            throw new PebbleException(
                    null,
                    kind.label + " [" + name + "] is not allowed for template ["
                            + templateName + "]",
                    lineNumber,
                    templateName);
        }
    }

    static Filter wrapFilter(String name, Filter filter, SandboxPolicyResolver policies) {
        if (filter instanceof DefaultFilter defaultFilter) {
            return new PolicyAwareDefaultFilter(defaultFilter, policies);
        }
        return new PolicyAwareFilter(name, filter, policies);
    }

    static Test wrapTest(String name, Test test, SandboxPolicyResolver policies) {
        if (test instanceof DefinedTest defined) {
            return new PolicyAwareDefinedTest(defined, policies);
        }
        return new PolicyAwareTest(name, test, policies);
    }
}
