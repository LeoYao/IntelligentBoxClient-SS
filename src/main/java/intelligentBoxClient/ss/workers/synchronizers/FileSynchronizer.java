package intelligentBoxClient.ss.workers.synchronizers;

import com.dropbox.core.v2.files.FileMetadata;
import intelligentBoxClient.ss.dao.pojo.DirectoryEntity;
import intelligentBoxClient.ss.dao.pojo.RemoteChangeEntity;
import intelligentBoxClient.ss.utils.Consts;
import intelligentBoxClient.ss.utils.IUtils;

import java.sql.Timestamp;

/**
 * Created by yaohx on 4/7/2016.
 */
public abstract class FileSynchronizer implements IFileSynchronizer {

    protected IUtils _utils;

    public FileSynchronizer(IUtils utils){
        _utils = utils;
    }

    protected DirectoryEntity getDirectoryEntity(RemoteChangeEntity change){
        DirectoryEntity entity = new DirectoryEntity();
        entity.setFullPath(change.getFullPath());
        entity.setParentFolderFullPath(_utils.extractParentFolderPath(change.getFullPath(), change.getEntryName()));
        entity.setEntryName(change.getEntryName());
        entity.setOldFullPath("");
        entity.setType(change.getType());
        entity.setSize(change.getSize());
        entity.setMtime(change.getMtime());
        entity.setAtime(change.getAtime());
        entity.setLocked(false);
        entity.setModified(false);
        entity.setLocal(false);
        entity.setDeleted(false);
        entity.setInUseCount(0);
        entity.setRevision(change.getRevision());
        return entity;
    }

    protected DirectoryEntity getDirectoryEntity(RemoteChangeEntity change, DirectoryEntity oldEntity){
        DirectoryEntity entity = new DirectoryEntity();
        entity.setFullPath(change.getFullPath());
        entity.setParentFolderFullPath(_utils.extractParentFolderPath(change.getFullPath(), change.getEntryName()));
        entity.setEntryName(change.getEntryName());
        entity.setOldFullPath(oldEntity.getOldFullPath());
        entity.setType(change.getType());
        entity.setSize(change.getSize());
        entity.setMtime(change.getMtime());
        entity.setAtime(change.getAtime());
        entity.setLocked(oldEntity.isLocked());
        entity.setModified(oldEntity.isModified());
        entity.setLocal(oldEntity.isLocal());
        entity.setDeleted(oldEntity.isDeleted());
        entity.setInUseCount(oldEntity.getInUseCount());
        entity.setRevision(change.getRevision());
        return entity;
    }

    protected DirectoryEntity getDirectoryEntity(FileMetadata metadata, DirectoryEntity oldEntity){
        DirectoryEntity entity = new DirectoryEntity();
        entity.setFullPath(metadata.getPathLower());
        entity.setParentFolderFullPath(_utils.extractParentFolderPath(metadata.getPathLower(), metadata.getName().toLowerCase()));
        entity.setEntryName(metadata.getName().toLowerCase());
        entity.setOldFullPath(oldEntity.getOldFullPath());
        entity.setType(Consts.FILE);
        entity.setSize(metadata.getSize());
        entity.setMtime(new Timestamp(metadata.getServerModified().getTime()));
        entity.setAtime(new Timestamp(metadata.getServerModified().getTime()));
        entity.setLocked(oldEntity.isLocked());
        entity.setModified(oldEntity.isModified());
        entity.setLocal(oldEntity.isLocal());
        entity.setDeleted(oldEntity.isDeleted());
        entity.setInUseCount(oldEntity.getInUseCount());
        entity.setRevision(metadata.getRev());
        return entity;
    }
}
