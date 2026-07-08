package edu.kit.pcm.architecture.serializer.core;

import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.repository.*;
import org.palladiosimulator.pcm.resourceenvironment.LinkingResource;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class PalladioArchitectureGeneralSerializer {
    /**
     * Serialize architecture with detailed information (default).
     */
    public static String serializeDetailed(
            Repository repository,
            System system,
            Allocation allocation,
            ResourceEnvironment resourceEnvironment) {

        StringBuilder sb = new StringBuilder();

        sb.append("Palladio Component Model Architecture\n");
        sb.append("=====================================\n\n");

        // Components with full details
        appendComponents(sb, repository);

        // Interfaces with operations
        appendInterfaces(sb, repository);

        // System assembly
        appendSystemAssembly(sb, system);

        // Connectors
        appendConnectors(sb, system);

        // Resource environment
        appendResourceEnvironment(sb, resourceEnvironment);

        // Allocation
        appendAllocation(sb, allocation);

        return sb.toString();
    }

    /**
     * Serialize architecture in compact format (minimal text).
     */
    public static String serializeCompact(
            Repository repository,
            System system,
            Allocation allocation,
            ResourceEnvironment resourceEnvironment) {

        StringBuilder sb = new StringBuilder();

        sb.append("Palladio Architecture:\n");

        // Components list
        sb.append("Components: [");
        sb.append(repository.getComponents__Repository().stream()
                .map(c -> c.getEntityName())
                .collect(Collectors.joining(", ")));
        sb.append("]\n");

        // Interfaces list
        sb.append("Interfaces: [");
        sb.append(repository.getInterfaces__Repository().stream()
                .map(i -> i.getEntityName())
                .collect(Collectors.joining(", ")));
        sb.append("]\n");

        // Allocation summary
        sb.append("Allocation: ");
        List<String> allocations = new ArrayList<>();
        for (AllocationContext ctx : allocation.getAllocationContexts_Allocation()) {
            AssemblyContext assemblyCtx = ctx.getAssemblyContext_AllocationContext();
            ResourceContainer container = ctx.getResourceContainer_AllocationContext();

            String assemblyName = (assemblyCtx != null) ? assemblyCtx.getEntityName() : "Unresolved";
            String containerName = (container != null) ? container.getEntityName() : "Unresolved";

            allocations.add(assemblyName + "->" + containerName);
        }
        sb.append(String.join(", ", allocations));
        sb.append("\n");

        // Resource environment summary
        sb.append("Resource Environment: ");
        List<String> resources = new ArrayList<>();
        for (ResourceContainer rc : resourceEnvironment.getResourceContainer_ResourceEnvironment()) {
            resources.add(rc.getEntityName());
        }
        sb.append(String.join(", ", resources));
        sb.append("\n");

        // Connectors summary
        sb.append("Connectors: ");
        List<String> connectors = new ArrayList<>();
        for (Connector conn : system.getConnectors__ComposedStructure()) {
            if (conn instanceof AssemblyConnector) {
                AssemblyConnector ac = (AssemblyConnector) conn;
                AssemblyContext requirer = ac.getRequiringAssemblyContext_AssemblyConnector();
                AssemblyContext provider = ac.getProvidingAssemblyContext_AssemblyConnector();

                String requirerName = (requirer != null) ? requirer.getEntityName() : "Unresolved";
                String providerName = (provider != null) ? provider.getEntityName() : "Unresolved";

                connectors.add(requirerName + "->" + providerName);
            }
        }
        sb.append(String.join(", ", connectors));
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Serialize architecture focusing on security-relevant aspects.
     */
    public static String serializeSecurityFocused(
            String description,
            Repository repository,
            System system,
            Allocation allocation,
            ResourceEnvironment resourceEnvironment) {

        StringBuilder sb = new StringBuilder();

        sb.append("Palladio Architecture - Security Analysis View\n");
        sb.append("==============================================\n\n");

        // Use case description
        sb.append("DESCRIPTION:\n");
        sb.append("-------------------\n");
        sb.append(description);
        sb.append("\n\n");

        // Components with interface exposure
        sb.append("COMPONENT EXPOSURE:\n");
        sb.append("-------------------\n");
        for (RepositoryComponent comp : repository.getComponents__Repository()) {
            if (comp instanceof BasicComponent) {
                BasicComponent bc = (BasicComponent) comp;
                sb.append(String.format("Component: %s\n", bc.getEntityName()));

                // Public interfaces (provided)
                if (!bc.getProvidedRoles_InterfaceProvidingEntity().isEmpty()) {
                    sb.append("  Exposed Interfaces:\n");
                    for (ProvidedRole role : bc.getProvidedRoles_InterfaceProvidingEntity()) {
                        if (role instanceof OperationProvidedRole) {
                            OperationProvidedRole opRole = (OperationProvidedRole) role;
                            OperationInterface iface = opRole.getProvidedInterface__OperationProvidedRole();
                            if (iface != null) {
                                List<String> operations = iface.getSignatures__OperationInterface().stream()
                                        .filter(sig -> sig != null)
                                        .map(sig -> sig.getEntityName())
                                        .collect(Collectors.toList());
                                String operationList = operations.isEmpty() ? "none" : String.join(", ", operations);
                                sb.append(String.format("    - %s (Operations: %s)\n",
                                        iface.getEntityName(), operationList));
                            } else {
                                sb.append(String.format("    - [Unresolved Interface: %s]\n", opRole.getEntityName()));
                            }
                        }
                    }
                }

                // Dependencies (required)
                if (!bc.getRequiredRoles_InterfaceRequiringEntity().isEmpty()) {
                    sb.append("  External Dependencies:\n");
                    for (RequiredRole role : bc.getRequiredRoles_InterfaceRequiringEntity()) {
                        if (role instanceof OperationRequiredRole) {
                            OperationRequiredRole opRole = (OperationRequiredRole) role;
                            OperationInterface iface = opRole.getRequiredInterface__OperationRequiredRole();
                            if (iface != null) {
                                sb.append(String.format("    - %s\n", iface.getEntityName()));
                            } else {
                                sb.append(String.format("    - [Unresolved Interface: %s]\n", opRole.getEntityName()));
                            }
                        }
                    }
                }
                sb.append("\n");
            }
        }

        // Communication paths
        sb.append("COMMUNICATION PATHS:\n");
        sb.append("--------------------\n");
        for (Connector conn : system.getConnectors__ComposedStructure()) {
            if (conn instanceof AssemblyConnector) {
                AssemblyConnector ac = (AssemblyConnector) conn;
                sb.append(String.format("- %s\n", conn.getEntityName()));

                AssemblyContext requirer = ac.getRequiringAssemblyContext_AssemblyConnector();
                AssemblyContext provider = ac.getProvidingAssemblyContext_AssemblyConnector();

                if (requirer != null) {
                    sb.append(String.format("    From: %s\n", requirer.getEntityName()));
                } else {
                    sb.append("    From: [Unresolved]\n");
                }

                if (provider != null) {
                    sb.append(String.format("    To: %s\n", provider.getEntityName()));
                } else {
                    sb.append("    To: [Unresolved]\n");
                }

                if (ac.getProvidedRole_AssemblyConnector() instanceof OperationProvidedRole) {
                    OperationProvidedRole role = (OperationProvidedRole) ac.getProvidedRole_AssemblyConnector();
                    OperationInterface iface = role.getProvidedInterface__OperationProvidedRole();
                    if (iface != null) {
                        sb.append(String.format("    Interface: %s\n", iface.getEntityName()));
                    } else {
                        sb.append(String.format("    Interface: [Unresolved: %s]\n", role.getEntityName()));
                    }
                }
            }
        }
        sb.append("\n");

        // Deployment topology
        sb.append("DEPLOYMENT TOPOLOGY:\n");
        sb.append("--------------------\n");

        // Network segments
        if (!resourceEnvironment.getLinkingResources__ResourceEnvironment().isEmpty()) {
            sb.append("Network Segments:\n");
            for (LinkingResource link : resourceEnvironment.getLinkingResources__ResourceEnvironment()) {
                sb.append(String.format("  - %s: ", link.getEntityName()));
                List<String> connected = new ArrayList<>();
                for (ResourceContainer rc : link.getConnectedResourceContainers_LinkingResource()) {
                    connected.add(rc.getEntityName());
                }
                sb.append(String.join(" <-> ", connected));
                sb.append("\n");
            }
        }

        // Component placement
        sb.append("\nComponent Deployment:\n");
        for (AllocationContext ctx : allocation.getAllocationContexts_Allocation()) {
            AssemblyContext assemblyCtx = ctx.getAssemblyContext_AllocationContext();
            ResourceContainer container = ctx.getResourceContainer_AllocationContext();

            String componentName = "[Unresolved Component]";
            if (assemblyCtx != null && assemblyCtx.getEncapsulatedComponent__AssemblyContext() != null) {
                componentName = assemblyCtx.getEncapsulatedComponent__AssemblyContext().getEntityName();
            }

            String serverName = "[Unresolved Server]";
            if (container != null) {
                serverName = container.getEntityName();
            }

            sb.append(String.format("  - %s deployed on %s\n", componentName, serverName));
        }

        return sb.toString();
    }

    // Helper methods for detailed serialization
    private static void appendComponents(StringBuilder sb, Repository repository) {
        sb.append("COMPONENTS:\n");
        sb.append("-----------\n");
        for (RepositoryComponent comp : repository.getComponents__Repository()) {
            if (comp instanceof BasicComponent) {
                BasicComponent bc = (BasicComponent) comp;
                sb.append(String.format("- %s (ID: %s)\n", bc.getEntityName(), bc.getId()));

                if (!bc.getProvidedRoles_InterfaceProvidingEntity().isEmpty()) {
                    sb.append("  Provides:\n");
                    for (ProvidedRole role : bc.getProvidedRoles_InterfaceProvidingEntity()) {
                        if (role instanceof OperationProvidedRole) {
                            OperationProvidedRole opRole = (OperationProvidedRole) role;
                            OperationInterface iface = opRole.getProvidedInterface__OperationProvidedRole();
                            if (iface != null) {
                                sb.append(String.format("    - %s\n", iface.getEntityName()));
                            } else {
                                sb.append(String.format("    - [Unresolved: %s]\n", opRole.getEntityName()));
                            }
                        }
                    }
                }

                if (!bc.getRequiredRoles_InterfaceRequiringEntity().isEmpty()) {
                    sb.append("  Requires:\n");
                    for (RequiredRole role : bc.getRequiredRoles_InterfaceRequiringEntity()) {
                        if (role instanceof OperationRequiredRole) {
                            OperationRequiredRole opRole = (OperationRequiredRole) role;
                            OperationInterface iface = opRole.getRequiredInterface__OperationRequiredRole();
                            if (iface != null) {
                                sb.append(String.format("    - %s\n", iface.getEntityName()));
                            } else {
                                sb.append(String.format("    - [Unresolved: %s]\n", opRole.getEntityName()));
                            }
                        }
                    }
                }
            }
        }
        sb.append("\n");
    }

    private static void appendInterfaces(StringBuilder sb, Repository repository) {
        sb.append("INTERFACES:\n");
        sb.append("-----------\n");
        for (org.palladiosimulator.pcm.repository.Interface iface : repository.getInterfaces__Repository()) {
            if (iface instanceof OperationInterface) {
                OperationInterface opIface = (OperationInterface) iface;
                sb.append(String.format("- %s\n", opIface.getEntityName()));
                for (OperationSignature sig : opIface.getSignatures__OperationInterface()) {
                    sb.append(String.format("    Operation: %s\n", sig.getEntityName()));
                }
            }
        }
        sb.append("\n");
    }

    private static void appendSystemAssembly(StringBuilder sb, System system) {
        sb.append("SYSTEM ASSEMBLY:\n");
        sb.append("----------------\n");
        sb.append(String.format("System Name: %s\n", system.getEntityName()));
        for (AssemblyContext ctx : system.getAssemblyContexts__ComposedStructure()) {
            sb.append(String.format("- Assembly: %s\n", ctx.getEntityName()));
            RepositoryComponent component = ctx.getEncapsulatedComponent__AssemblyContext();
            if (component != null) {
                sb.append(String.format("    Component: %s\n", component.getEntityName()));
            } else {
                sb.append("    Component: [Unresolved]\n");
            }
        }
        sb.append("\n");
    }

    private static void appendConnectors(StringBuilder sb, System system) {
        sb.append("CONNECTORS:\n");
        sb.append("-----------\n");
        for (Connector conn : system.getConnectors__ComposedStructure()) {
            if (conn instanceof AssemblyConnector) {
                AssemblyConnector asmConn = (AssemblyConnector) conn;
                AssemblyContext requirer = asmConn.getRequiringAssemblyContext_AssemblyConnector();
                AssemblyContext provider = asmConn.getProvidingAssemblyContext_AssemblyConnector();

                String requirerName = (requirer != null) ? requirer.getEntityName() : "[Unresolved]";
                String providerName = (provider != null) ? provider.getEntityName() : "[Unresolved]";

                sb.append(String.format("- %s: %s -> %s\n",
                        conn.getEntityName(), requirerName, providerName));
            }
        }
        sb.append("\n");
    }

    private static void appendResourceEnvironment(StringBuilder sb, ResourceEnvironment resourceEnv) {
        sb.append("RESOURCE ENVIRONMENT:\n");
        sb.append("--------------------\n");
        for (ResourceContainer container : resourceEnv.getResourceContainer_ResourceEnvironment()) {
            sb.append(String.format("- %s (ID: %s)\n", container.getEntityName(), container.getId()));
        }

        if (!resourceEnv.getLinkingResources__ResourceEnvironment().isEmpty()) {
            sb.append("\nNetworks:\n");
            for (LinkingResource link : resourceEnv.getLinkingResources__ResourceEnvironment()) {
                sb.append(String.format("- %s connects: ", link.getEntityName()));
                List<String> containerNames = new ArrayList<>();
                for (ResourceContainer rc : link.getConnectedResourceContainers_LinkingResource()) {
                    containerNames.add(rc.getEntityName());
                }
                sb.append(String.join(", ", containerNames));
                sb.append("\n");
            }
        }
        sb.append("\n");
    }

    private static void appendAllocation(StringBuilder sb, Allocation allocation) {
        sb.append("ALLOCATION MODEL:\n");
        sb.append("-----------------\n");
        for (AllocationContext allocCtx : allocation.getAllocationContexts_Allocation()) {
            AssemblyContext assemblyCtx = allocCtx.getAssemblyContext_AllocationContext();
            ResourceContainer container = allocCtx.getResourceContainer_AllocationContext();

            String assemblyName = (assemblyCtx != null) ? assemblyCtx.getEntityName() : "[Unresolved]";
            String containerName = (container != null) ? container.getEntityName() : "[Unresolved]";

            sb.append(String.format("- %s -> %s\n", assemblyName, containerName));
        }
    }

}
