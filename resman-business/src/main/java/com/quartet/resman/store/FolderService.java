package com.quartet.resman.store;

import com.quartet.resman.entity.Document;
import com.quartet.resman.entity.Entry;
import com.quartet.resman.entity.Folder;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.jcr.JcrCallback;
import org.springframework.extensions.jcr.JcrTemplate;
import org.springframework.extensions.jcr.jackrabbit.ocm.JcrMappingCallback;
import org.springframework.extensions.jcr.jackrabbit.ocm.JcrMappingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author lcheng
 * @version 1.0
 *          ${tags}
 */
@Service
@Transactional(propagation = Propagation.REQUIRED)
public class FolderService {

    @Autowired
    private JcrMappingTemplate mappingTemplate;

    @Autowired
    private JcrTemplate jcrTemplate;

    public void addFolder(Folder folder) {
        mappingTemplate.insert(folder);
        mappingTemplate.save();
    }

    public void updateFolder(Folder folder){
        mappingTemplate.update(folder);
        mappingTemplate.save();
    }

    public void deleteFolder(String path) {
        boolean exist = mappingTemplate.itemExists(path);
        if (exist) {
            mappingTemplate.remove(path);
            mappingTemplate.save();
        }
    }

    public void deleteFolderByUuid(String uuid) {
        final Node node = mappingTemplate.getNodeByIdentifier(uuid);
        if (node != null) {
            mappingTemplate.execute(new JcrCallback<Object>() {
                @Override
                public Object doInJcr(Session session) throws IOException, RepositoryException {
                    node.remove();
                    session.save();
                    return null;
                }
            });
        }
    }

    public void rename(String path, String newName) {
        try {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            Node node = mappingTemplate.getRootNode().getNode(path);
            if (node != null) {
                mappingTemplate.rename(node, newName);
                mappingTemplate.save();
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * 移动节点到某个节点
     * @param srcPath
     * @param destPath
     */
    public void moveTo(final String srcPath,final String destPath){
        mappingTemplate.execute(new JcrCallback<Object>() {
            @Override
            public Object doInJcr(Session session) throws IOException, RepositoryException {
                session.move(srcPath,destPath);
                session.save();
                return null;
            }
        });

    }

    /**
     * Copy某个节点到目标节点下面
     * @param srcPath
     * @param destPath
     */
    public void copyTo(final String srcPath, final String destPath){
        mappingTemplate.execute(new JcrCallback<Object>() {
            @Override
            public Object doInJcr(Session session) throws IOException, RepositoryException {
                session.getWorkspace().copy(srcPath, destPath);
                session.save();
                return null;
            }
        });
    }

    @Transactional(readOnly = true)
    public Folder getFolder(String path) {
        checkNotNull(path);
        Object obj = mappingTemplate.getObject(path);
        if (obj != null) {
            return (Folder) obj;
        }
        return null;
    }

    @Transactional(readOnly = true)
    public Folder getFolderByUUID(final String uuid) {
        checkNotNull(uuid);
        return mappingTemplate.execute(new JcrMappingCallback<Folder>() {
            @Override
            public Folder doInJcrMapping(ObjectContentManager manager) throws JcrMappingException {
                return (Folder) manager.getObjectByUuid(uuid);
            }
        });
    }

    @Transactional(readOnly = true)
    public Object getObjectByUUID(final String uid){
        return mappingTemplate.execute(new JcrMappingCallback<Object>() {
            @Override
            public Object doInJcrMapping(ObjectContentManager manager) throws JcrMappingException {
                return manager.getObjectByUuid(uid);
            }
        });
    }

    /**
     * 性能考虑，没有使用OCM的自动Mapping功能。
     * OCM级联获取嵌套的子目录，当结构比较复杂时性能会成问题。
     *
     * @param path
     * @return
     */
    @Transactional(readOnly = true)
    public List<Entry> getChildren(final String path) {

        return mappingTemplate.execute(new JcrCallback<List<Entry>>() {
            @Override
            public List<Entry> doInJcr(Session session) throws IOException, RepositoryException {
                Node root = session.getRootNode();
                String tempPath = path;
                if (path.startsWith("/")) {
                    tempPath = path.substring(1);
                }
                Node node = root.getNode(tempPath);
                List<Entry> result = new ArrayList<Entry>();
                if (node != null) {
                    for (NodeIterator it = node.getNodes(); it.hasNext(); ) {
                        Node n = it.nextNode();
                        Entry entry = NodeMapper.mapEntry(n);
                        if (entry != null) {
                            result.add(entry);
                        }
                    }
                }
                return result;
            }
        });
    }

    @Transactional(readOnly = true)
    public Map<String,Object> getChildren(final String path,final String nameLike,final long from,final long size){
        return mappingTemplate.execute(new JcrCallback<Map<String,Object>>() {
            @Override
            public Map<String,Object> doInJcr(Session session) throws IOException, RepositoryException {
                String tempPath = path;
                if(!path.startsWith("/")){
                    tempPath = "/"+path;
                }

                javax.jcr.query.QueryManager qm = session.getWorkspace().getQueryManager();
                StringBuilder sqlsb = new StringBuilder();
                sqlsb.append("SELECT * FROM [rm:entry] AS f WHERE ISCHILDNODE(f,'");
                sqlsb.append(tempPath+"') ");
                if (StringUtils.isNotEmpty(nameLike)){
                    sqlsb.append("AND LOCALNAME(f) LIKE '%"+nameLike+"%' ");
                }
                javax.jcr.query.Query query = qm.createQuery(sqlsb.toString(),javax.jcr.query.Query.JCR_SQL2);
                long total = query.execute().getNodes().getSize();

                query = qm.createQuery(sqlsb.toString(),javax.jcr.query.Query.JCR_SQL2);
                query.setOffset(from);
                query.setLimit(size);

                Map<String,Object> result = new HashMap<>();
                List<Entry> rows = new ArrayList<>();

                QueryResult nodeResult = query.execute();
                for (NodeIterator it = nodeResult.getNodes(); it.hasNext(); ) {
                    Node n = it.nextNode();
                    Entry entry = NodeMapper.mapEntry(n);
                    rows.add(entry);
                }
                result.put("total",total);
                result.put("rows",rows);
                return result;
            }
        });
    }

    /**
     *
     * @param path
     * @return
     */
    @Transactional(readOnly = true)
    public List<Folder> getChildrenFolders(final String path){

        return mappingTemplate.execute(new JcrCallback<List<Folder>>() {
            @Override
            public List<Folder> doInJcr(Session session) throws IOException, RepositoryException {
                String tempPath = path;
                if(!path.startsWith("/")){
                    tempPath = "/"+path;
                }
                List<Folder> result = new ArrayList();

                javax.jcr.query.QueryManager qm = session.getWorkspace().getQueryManager();
                String sql = "SELECT * FROM [rm:folder] AS f where ISCHILDNODE(f,'"+tempPath+"')";
                javax.jcr.query.Query query = qm.createQuery(sql, javax.jcr.query.Query.JCR_SQL2);
                QueryResult nodeResult = query.execute();
                for (NodeIterator it = nodeResult.getNodes(); it.hasNext(); ) {
                    Node n = it.nextNode();
                    Entry entry = NodeMapper.mapEntry(n);
                    if (entry != null && entry instanceof Folder) {
                        result.add((Folder)entry);
                    }
                }
                return result;
            }
        });
    }

    public List<Document> getChildrenFiles(final String path){
        return mappingTemplate.execute(new JcrCallback<List<Document>>() {
            @Override
            public List<Document> doInJcr(Session session) throws IOException, RepositoryException {
                String tempPath = path;
                if(!path.startsWith("/")){
                    tempPath = "/"+path;
                }
                List<Document> result = new ArrayList();

                javax.jcr.query.QueryManager qm = session.getWorkspace().getQueryManager();
                String sql = "SELECT * FROM [rm:file] AS f where ISCHILDNODE(f,'"+tempPath+"')";
                javax.jcr.query.Query query = qm.createQuery(sql, javax.jcr.query.Query.JCR_SQL2);
                QueryResult nodeResult = query.execute();
                for (NodeIterator it = nodeResult.getNodes(); it.hasNext(); ) {
                    Node n = it.nextNode();
                    Entry entry = NodeMapper.mapEntry(n);
                    if (entry != null && entry instanceof Document) {
                        result.add((Document)entry);
                    }
                }
                return result;
            }
        });
    }

    /**
     * 按status 和 visibility 过滤查询子节点
     *
     * @param path
     * @param status
     * @param visibility
     * @return
     */
    @Transactional(readOnly = true)
    public List<Entry> getChildren(String path, String status, String visibility) {
        if (path != null && !path.endsWith("/")) {
            path = path + "/";
        }
        QueryManager qm = mappingTemplate.createQueryManager();
        Filter filter = qm.createFilter(Entry.class);
        filter.setScope(path);

        filter.addEqualTo("status", status);
        filter.addEqualTo("visibility", visibility);
        Query q = qm.createQuery(filter);
        return (List<Entry>) mappingTemplate.getObjects(q);
    }

    public String processPath(String path){
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }
}
