package edu.kit.pcm.architecture.serializer.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.repository.*;
import org.palladiosimulator.pcm.resourceenvironment.LinkingResource;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;

import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Utility class for serializing Palladio architecture models into text representations.
 * Provides Json-like serialization strategy
 */
public class PalladioArchitectureSerializer {

    /* Architecture Serializer in Json Format */
    public static String serializeAsJson(
            Repository repository,
            org.palladiosimulator.pcm.system.System system,
            Allocation allocation,
            ResourceEnvironment resourceEnvironment) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            ObjectNode root = mapper.createObjectNode();
            root.put("modelType", "Palladio Component Model Architecture");

            root.set("components", buildComponentsJson(mapper, repository));
            root.set("interfaces", buildInterfacesJson(mapper, repository));
            root.set("systemAssembly", buildSystemAssemblyJson(mapper, system));
            root.set("connectors", buildConnectorsJson(mapper, system));
            root.set("resourceEnvironment", buildResourceEnvironmentJson(mapper, resourceEnvironment));
            root.set("allocation", buildAllocationJson(mapper, allocation));

            return mapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize architecture as JSON", e);
        }
    }

    private static ArrayNode buildComponentsJson(ObjectMapper mapper, Repository repository) {
        ArrayNode componentsArray = mapper.createArrayNode();

        for (RepositoryComponent component : repository.getComponents__Repository()) {
            ObjectNode componentNode = mapper.createObjectNode();
            componentNode.put("name", safe(component.getEntityName()));

            Set<String> provides = new LinkedHashSet<>();
            for (ProvidedRole role : component.getProvidedRoles_InterfaceProvidingEntity()) {
                String interfaceName = extractProvidedInterfaceName(role);
                if (!interfaceName.isBlank()) {
                    provides.add(interfaceName);
                }
            }
            componentNode.set("provides", toStringArray(mapper, provides));

            Set<String> requires = new LinkedHashSet<>();
            for (RequiredRole role : component.getRequiredRoles_InterfaceRequiringEntity()) {
                String interfaceName = extractRequiredInterfaceName(role);
                if (!interfaceName.isBlank()) {
                    requires.add(interfaceName);
                }
            }
            componentNode.set("requires", toStringArray(mapper, requires));

            componentsArray.add(componentNode);
        }

        return componentsArray;
    }

    private static ArrayNode buildInterfacesJson(ObjectMapper mapper, Repository repository) {
        ArrayNode interfacesArray = mapper.createArrayNode();

        for (var iface : repository.getInterfaces__Repository()) {
            if (iface instanceof OperationInterface opInterface) {
                ObjectNode interfaceNode = mapper.createObjectNode();
                interfaceNode.put("name", safe(opInterface.getEntityName()));

                Set<String> operations = new LinkedHashSet<>();
                for (OperationSignature sig : opInterface.getSignatures__OperationInterface()) {
                    String operationName = safe(sig.getEntityName());
                    if (!operationName.isBlank()) {
                        operations.add(operationName);
                    }
                }

                interfaceNode.set("operations", toStringArray(mapper, operations));
                interfacesArray.add(interfaceNode);
            }
        }

        return interfacesArray;
    }

    private static ObjectNode buildSystemAssemblyJson(
            ObjectMapper mapper,
            org.palladiosimulator.pcm.system.System system) {

        ObjectNode systemNode = mapper.createObjectNode();
        systemNode.put("systemName", safe(system.getEntityName()));

        ArrayNode assemblyArray = mapper.createArrayNode();

        for (AssemblyContext ctx : system.getAssemblyContexts__ComposedStructure()) {
            ObjectNode ctxNode = mapper.createObjectNode();
            ctxNode.put("name", safe(ctx.getEntityName()));

            if (ctx.getEncapsulatedComponent__AssemblyContext() != null) {
                RepositoryComponent component = ctx.getEncapsulatedComponent__AssemblyContext();

                ctxNode.put("component", safe(component.getEntityName()));
                ctxNode.put("componentType", component.eClass().getName());

                if (isCompositeComponent(component)) {
                    ctxNode.set("innerStructure", buildCompositeStructureJson(mapper, component));
                }
            }

            assemblyArray.add(ctxNode);
        }

        systemNode.set("assemblies", assemblyArray);
        return systemNode;
    }

    private static ObjectNode buildCompositeStructureJson(
            ObjectMapper mapper,
            RepositoryComponent component) {

        ObjectNode compositeNode = mapper.createObjectNode();

        if (!(component instanceof org.palladiosimulator.pcm.repository.CompositeComponent composite)) {
            return compositeNode;
        }

        ArrayNode assemblies = mapper.createArrayNode();
        for (AssemblyContext innerCtx : composite.getAssemblyContexts__ComposedStructure()) {
            ObjectNode innerNode = mapper.createObjectNode();
            innerNode.put("name", safe(innerCtx.getEntityName()));

            if (innerCtx.getEncapsulatedComponent__AssemblyContext() != null) {
                RepositoryComponent innerComponent = innerCtx.getEncapsulatedComponent__AssemblyContext();
                innerNode.put("component", safe(innerComponent.getEntityName()));
                innerNode.put("componentType", innerComponent.eClass().getName());

                if (isCompositeComponent(innerComponent)) {
                    innerNode.set("innerStructure", buildCompositeStructureJson(mapper, innerComponent));
                }
            }

            assemblies.add(innerNode);
        }

        ArrayNode connectors = mapper.createArrayNode();
        for (Connector connector : composite.getConnectors__ComposedStructure()) {
            if (connector instanceof AssemblyConnector ac) {
                ObjectNode connectorNode = mapper.createObjectNode();
                connectorNode.put("name", safe(ac.getEntityName()));
                if (ac.getRequiringAssemblyContext_AssemblyConnector() != null) {
                    connectorNode.put("from",
                            safe(ac.getRequiringAssemblyContext_AssemblyConnector().getEntityName()));
                }
                if (ac.getProvidingAssemblyContext_AssemblyConnector() != null) {
                    connectorNode.put("to",
                            safe(ac.getProvidingAssemblyContext_AssemblyConnector().getEntityName()));
                }
                String iface = extractConnectorInterfaceName(ac);
                if (!iface.isBlank()) {
                    connectorNode.put("interface", iface);
                }
                connectors.add(connectorNode);
            }
        }

        compositeNode.set("assemblies", assemblies);
        compositeNode.set("connectors", connectors);
        return compositeNode;
    }

    private static boolean isCompositeComponent(RepositoryComponent component) {
        return component instanceof org.palladiosimulator.pcm.repository.CompositeComponent;
    }


    private static ArrayNode buildConnectorsJson(
            ObjectMapper mapper,
            org.palladiosimulator.pcm.system.System system) {

        ArrayNode connectorsArray = mapper.createArrayNode();

        for (Connector connector : system.getConnectors__ComposedStructure()) {
            if (!(connector instanceof AssemblyConnector ac)) {
                continue; // drop shallow/uninformative delegation connectors
            }

            ObjectNode connectorNode = mapper.createObjectNode();
            connectorNode.put("name", safe(connector.getEntityName()));

            if (ac.getRequiringAssemblyContext_AssemblyConnector() != null) {
                connectorNode.put("from",
                        safe(ac.getRequiringAssemblyContext_AssemblyConnector().getEntityName()));
            }

            if (ac.getProvidingAssemblyContext_AssemblyConnector() != null) {
                connectorNode.put("to",
                        safe(ac.getProvidingAssemblyContext_AssemblyConnector().getEntityName()));
            }

            String interfaceName = extractConnectorInterfaceName(ac);
            if (!interfaceName.isBlank()) {
                connectorNode.put("interface", interfaceName);
            }

            connectorsArray.add(connectorNode);
        }

        return connectorsArray;
    }

    private static ObjectNode buildResourceEnvironmentJson(
            ObjectMapper mapper,
            ResourceEnvironment resourceEnvironment) {

        ObjectNode resourceEnvNode = mapper.createObjectNode();

        Set<String> resourceContainers = new LinkedHashSet<>();
        for (ResourceContainer container : resourceEnvironment.getResourceContainer_ResourceEnvironment()) {
            String containerName = safe(container.getEntityName());
            if (!containerName.isBlank()) {
                resourceContainers.add(containerName);
            }
        }
        resourceEnvNode.set("resourceContainers", toStringArray(mapper, resourceContainers));

        ArrayNode networksArray = mapper.createArrayNode();
        for (LinkingResource network : resourceEnvironment.getLinkingResources__ResourceEnvironment()) {
            ObjectNode networkNode = mapper.createObjectNode();
            networkNode.put("name", safe(network.getEntityName()));

            Set<String> connectedNames = new LinkedHashSet<>();
            for (ResourceContainer container : network.getConnectedResourceContainers_LinkingResource()) {
                String containerName = safe(container.getEntityName());
                if (!containerName.isBlank()) {
                    connectedNames.add(containerName);
                }
            }

            networkNode.set("connects", toStringArray(mapper, connectedNames));
            networksArray.add(networkNode);
        }

        resourceEnvNode.set("networks", networksArray);
        return resourceEnvNode;
    }

    private static ArrayNode buildAllocationJson(ObjectMapper mapper, Allocation allocation) {
        ArrayNode allocationArray = mapper.createArrayNode();

        for (AllocationContext ctx : allocation.getAllocationContexts_Allocation()) {
            ObjectNode ctxNode = mapper.createObjectNode();

            if (ctx.getAssemblyContext_AllocationContext() != null) {
                ctxNode.put("assembly",
                        safe(ctx.getAssemblyContext_AllocationContext().getEntityName()));
            }

            if (ctx.getResourceContainer_AllocationContext() != null) {
                ctxNode.put("resourceContainer",
                        safe(ctx.getResourceContainer_AllocationContext().getEntityName()));
            }

            allocationArray.add(ctxNode);
        }

        return allocationArray;
    }

    private static ArrayNode toStringArray(ObjectMapper mapper, Iterable<String> values) {
        ArrayNode arrayNode = mapper.createArrayNode();

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                arrayNode.add(value);
            }
        }

        return arrayNode;
    }

    private static String extractProvidedInterfaceName(ProvidedRole role) {
        if (role instanceof OperationProvidedRole opRole &&
                opRole.getProvidedInterface__OperationProvidedRole() != null) {
            return safe(opRole.getProvidedInterface__OperationProvidedRole().getEntityName());
        }
        return "";
    }

    private static String extractRequiredInterfaceName(RequiredRole role) {
        if (role instanceof OperationRequiredRole opRole &&
                opRole.getRequiredInterface__OperationRequiredRole() != null) {
            return safe(opRole.getRequiredInterface__OperationRequiredRole().getEntityName());
        }
        return "";
    }

    private static String extractConnectorInterfaceName(AssemblyConnector ac) {
        if (ac.getProvidedRole_AssemblyConnector() instanceof OperationProvidedRole providedRole &&
                providedRole.getProvidedInterface__OperationProvidedRole() != null) {
            return safe(providedRole.getProvidedInterface__OperationProvidedRole().getEntityName());
        }

        if (ac.getRequiredRole_AssemblyConnector() instanceof OperationRequiredRole requiredRole &&
                requiredRole.getRequiredInterface__OperationRequiredRole() != null) {
            return safe(requiredRole.getRequiredInterface__OperationRequiredRole().getEntityName());
        }

        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

}