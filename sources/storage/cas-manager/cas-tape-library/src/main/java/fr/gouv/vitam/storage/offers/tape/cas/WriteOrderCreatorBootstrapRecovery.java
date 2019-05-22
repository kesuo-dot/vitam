/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.ExtendedFileOutputStream;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeTarReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.TarReferentialException;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.getCreationDateFromTarId;

public class WriteOrderCreatorBootstrapRecovery {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WriteOrderCreatorBootstrapRecovery.class);

    private final String inputTarStorageFolder;
    private final TarReferentialRepository tarReferentialRepository;
    private final BucketTopologyHelper bucketTopologyHelper;
    private final WriteOrderCreator writeOrderCreator;
    private final TarFileRapairer tarFileRapairer;

    public WriteOrderCreatorBootstrapRecovery(
        String inputTarStorageFolder,
        TarReferentialRepository tarReferentialRepository,
        BucketTopologyHelper bucketTopologyHelper,
        WriteOrderCreator writeOrderCreator, TarFileRapairer tarFileRapairer) {
        this.inputTarStorageFolder = inputTarStorageFolder;
        this.tarReferentialRepository = tarReferentialRepository;
        this.bucketTopologyHelper = bucketTopologyHelper;
        this.writeOrderCreator = writeOrderCreator;
        this.tarFileRapairer = tarFileRapairer;
    }

    public void initializeOnBootstrap() {

        try {

            for (String fileBucket : bucketTopologyHelper.listFileBuckets()) {

                Path fileBucketTarStoragePath = Paths.get(inputTarStorageFolder).resolve(fileBucket);
                if (fileBucketTarStoragePath.toFile().exists()) {
                    recoverFileBucketTars(fileBucket, fileBucketTarStoragePath);
                }

            }
        } catch (Exception e) {
            throw new VitamRuntimeException("Could not reschedule tar files to copy on tape", e);
        }
    }

    private void recoverFileBucketTars(String fileBucket, Path fileBucketTarStoragePath)
        throws IOException, TarReferentialException, ObjectReferentialException, QueueException {

        Map<String, FileGroup> tarFileGroups = getFileListGroupedByTarId(fileBucketTarStoragePath);

        List<String> tarFileNames = cleanupIncompleteFiles(fileBucketTarStoragePath, tarFileGroups);

        // Sort files by creation date
        sortFilesByCreationDate(tarFileNames);

        // Process tar archives
        for (String tarFileName : tarFileNames) {
            if (tarFileName.endsWith(LocalFileUtils.TAR_EXTENSION)) {
                processReadyTar(fileBucket, fileBucketTarStoragePath, tarFileName);
            } else if (tarFileName.endsWith(LocalFileUtils.TMP_EXTENSION)) {
                repairTarArchive(fileBucketTarStoragePath, tarFileName, fileBucket);
            } else {
                throw new IllegalStateException("Invalid file extension " + tarFileName);
            }
        }
    }

    private Map<String, FileGroup> getFileListGroupedByTarId(Path fileBucketTarStoragePath) throws IOException {

        // List tar file paths
        Map<String, FileGroup> tarFileNames = new HashMap<>();
        try (Stream<Path> tarFileStream = Files.list(fileBucketTarStoragePath)) {
            tarFileStream
                .map(filePath -> filePath.getFileName().toString())
                .forEach(tarFileName -> {

                    // Group files by tar id
                    String tarId = LocalFileUtils.tarFileNamePathToTarId(tarFileName);
                    FileGroup fileGroup = tarFileNames.computeIfAbsent(tarId, f -> new FileGroup());

                    if (tarFileName.endsWith(LocalFileUtils.TMP_EXTENSION)) {
                        fileGroup.tmpFileName = tarFileName;
                    } else {
                        fileGroup.readyTarFileName = tarFileName;
                    }
                });
        }
        return tarFileNames;
    }

    private List<String> cleanupIncompleteFiles(Path fileBucketTarStoragePath,
        Map<String, FileGroup> tarFileGroups) throws IOException {

        /* Delete incomplete files
         * > X.tar.tmp + X.tar          : Delete incomplete .tar
         * > X.tar.tmp                  : NOP
         * > X.tar                      : NOP
         */

        List<String> tarFileNames = new ArrayList<>();
        for (FileGroup fileGroup : tarFileGroups.values()) {

            if (fileGroup.tmpFileName != null) {

                if (fileGroup.readyTarFileName != null) {
                    LOGGER.warn("Deleting incomplete file " + fileGroup.readyTarFileName);
                    Files.delete(fileBucketTarStoragePath.resolve(fileGroup.readyTarFileName));
                    fileGroup.readyTarFileName = null;
                }

                tarFileNames.add(fileGroup.tmpFileName);

            } else {

                LOGGER.info("Found ready file " + fileGroup.readyTarFileName);
                tarFileNames.add(fileGroup.readyTarFileName);
            }
        }
        return tarFileNames;
    }

    private void sortFilesByCreationDate(List<String> tarFileNames) {
        tarFileNames.sort((filename1, filename2) -> {

            String tarId1 = LocalFileUtils.tarFileNamePathToTarId(filename1);
            String tarId2 = LocalFileUtils.tarFileNamePathToTarId(filename1);

            String creationDate1 = getCreationDateFromTarId(tarId1);
            String creationDate2 = getCreationDateFromTarId(tarId2);

            int compare = creationDate1.compareTo(creationDate2);
            if (compare != 0)
                return compare;
            return filename1.compareTo(filename2);
        });
    }

    private void processReadyTar(String fileBucket, Path fileBucketTarStoragePath, String tarId)
        throws TarReferentialException, IOException, ObjectReferentialException, QueueException {

        Path tarFile = fileBucketTarStoragePath.resolve(tarId);

        Optional<TapeTarReferentialEntity> tarReferentialEntity =
            tarReferentialRepository.find(tarId);
        if (!tarReferentialEntity.isPresent()) {
            throw new IllegalStateException(
                "Unknown tar file in tar referential '" + tarFile.toString() + "'");
        }

        if (tarReferentialEntity.get()
            .getLocation() instanceof TapeLibraryOnTapeTarStorageLocation) {

            LOGGER.warn("Tar file {} already written on tape. Deleting it", tarFile);
            Files.delete(tarFile);

        } else if (tarReferentialEntity.get().getLocation()
            instanceof TapeLibraryReadyOnDiskTarStorageLocation) {

            LOGGER.warn("Rescheduling tar file {} for copy on tape.", tarFile);
            WriteOrder message = new WriteOrder(
                bucketTopologyHelper.getBucketFromFileBucket(fileBucket),
                LocalFileUtils.tarFileNameRelativeToInputTarStorageFolder(fileBucket, tarId),
                tarReferentialEntity.get().getSize(),
                tarReferentialEntity.get().getDigestValue(),
                tarId
            );

            writeOrderCreator.sendMessageToQueue(message);

        } else if (tarReferentialEntity.get().getLocation()
            instanceof TapeLibraryBuildingOnDiskTarStorageLocation) {

            LOGGER.warn("Check tar file & compute size & digest.", tarFile);
            TarFileRapairer.DigestWithSize digestWithSize = verifyTarArchive(tarFile);

            // Add to queue
            WriteOrder message = new WriteOrder(
                bucketTopologyHelper.getBucketFromFileBucket(fileBucket),
                LocalFileUtils.tarFileNameRelativeToInputTarStorageFolder(fileBucket, tarId),
                digestWithSize.getSize(),
                digestWithSize.getDigestValue(),
                tarId
            );
            writeOrderCreator.sendMessageToQueue(message);

        } else {
            throw new IllegalStateException(
                "Invalid tar location " + tarReferentialEntity.get().getLocation().getClass()
                    + " (" + JsonHandler.unprettyPrint(tarReferentialEntity) + ")");
        }
    }

    private TarFileRapairer.DigestWithSize verifyTarArchive(Path tarFile)
        throws IOException, ObjectReferentialException {

        try (InputStream inputStream = Files.newInputStream(tarFile, StandardOpenOption.READ)) {
            return tarFileRapairer.verifyTarArchive(inputStream);
        }
    }

    private void repairTarArchive(Path fileBucketTarStoragePath, String tmpTarFileName, String fileBucket)
        throws IOException, ObjectReferentialException, QueueException, TarReferentialException {

        String tarId = LocalFileUtils.tarFileNamePathToTarId(tmpTarFileName);

        Path tmpTarFilePath = fileBucketTarStoragePath.resolve(tmpTarFileName);
        Path finalFilePath = fileBucketTarStoragePath.resolve(tarId);

        LOGGER.info("Repairing & verifying file " + tmpTarFilePath);

        TarFileRapairer.DigestWithSize digestWithSize;
        try (InputStream inputStream = Files.newInputStream(tmpTarFilePath, StandardOpenOption.READ);
            OutputStream outputStream = new ExtendedFileOutputStream(finalFilePath, true)) {

            digestWithSize = tarFileRapairer.repairAndVerifyTarArchive(inputStream, outputStream, tarId);
        }

        Files.delete(tmpTarFilePath);
        LOGGER.info("Successfully repaired & verified file " + tmpTarFilePath + " to " + finalFilePath);

        // Add to queue
        WriteOrder message = new WriteOrder(
            bucketTopologyHelper.getBucketFromFileBucket(fileBucket),
            LocalFileUtils.tarFileNameRelativeToInputTarStorageFolder(fileBucket, tarId),
            digestWithSize.getSize(),
            digestWithSize.getDigestValue(),
            tarId
        );
        writeOrderCreator.sendMessageToQueue(message);
    }

    private static class FileGroup {
        private String readyTarFileName;
        private String tmpFileName;
    }
}