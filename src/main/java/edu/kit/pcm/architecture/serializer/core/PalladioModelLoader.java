package edu.kit.pcm.architecture.serializer.core;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationPackage;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentPackage;
import org.palladiosimulator.pcm.system.SystemPackage;

import java.util.Map;

public class PalladioModelLoader {

    public static PalladioArchitecture load(String folderPath, String modelBaseName) {
        initEmf();

        ResourceSet resourceSet = new ResourceSetImpl();

        Resource repositoryResource = resourceSet.getResource(
                URI.createFileURI(folderPath + "/" + modelBaseName + ".repository"), true);

        Resource systemResource = resourceSet.getResource(
                URI.createFileURI(folderPath + "/" + modelBaseName + ".system"), true);

        Resource allocationResource = resourceSet.getResource(
                URI.createFileURI(folderPath + "/" + modelBaseName + ".allocation"), true);

        Resource resourceEnvironmentResource = resourceSet.getResource(
                URI.createFileURI(folderPath + "/" + modelBaseName + ".resourceenvironment"), true);

        EcoreUtil.resolveAll(resourceSet);

        Repository repository = (Repository) repositoryResource.getContents().get(0);

        org.palladiosimulator.pcm.system.System system =
                (org.palladiosimulator.pcm.system.System) systemResource.getContents().get(0);

        Allocation allocation = (Allocation) allocationResource.getContents().get(0);

        ResourceEnvironment resourceEnvironment =
                (ResourceEnvironment) resourceEnvironmentResource.getContents().get(0);

        return new PalladioArchitecture(repository, system, allocation, resourceEnvironment);
    }

    private static void initEmf() {
        Map<String, Object> extMap = Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap();
        XMIResourceFactoryImpl xmiFactory = new XMIResourceFactoryImpl();

        extMap.put("repository", xmiFactory);
        extMap.put("system", xmiFactory);
        extMap.put("allocation", xmiFactory);
        extMap.put("resourceenvironment", xmiFactory);

        RepositoryPackage.eINSTANCE.eClass();
        SystemPackage.eINSTANCE.eClass();
        AllocationPackage.eINSTANCE.eClass();
        ResourceenvironmentPackage.eINSTANCE.eClass();
    }

    public static class PalladioArchitecture {
        public final Repository repository;
        public final org.palladiosimulator.pcm.system.System system;
        public final Allocation allocation;
        public final ResourceEnvironment resourceEnvironment;

        public PalladioArchitecture(
                Repository repository,
                org.palladiosimulator.pcm.system.System system,
                Allocation allocation,
                ResourceEnvironment resourceEnvironment) {
            this.repository = repository;
            this.system = system;
            this.allocation = allocation;
            this.resourceEnvironment = resourceEnvironment;
        }
    }
}