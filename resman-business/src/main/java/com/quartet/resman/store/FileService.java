package com.quartet.resman.store;

import com.quartet.resman.entity.Document;
import com.quartet.resman.entity.Entry;
import com.quartet.resman.utils.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.jcr.JcrCallback;
import org.springframework.extensions.jcr.jackrabbit.ocm.JcrMappingCallback;
import org.springframework.extensions.jcr.jackrabbit.ocm.JcrMappingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jcr.*;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
public class FileService {

    @Autowired
    private JcrMappingTemplate mappingTemplate;

    public void addFile(Document document) {
        mappingTemplate.insert(document);
        mappingTemplate.save();
    }

    public void updateFile(Document document) {
        mappingTemplate.update(document);
        mappingTemplate.save();
    }

    /**
     * 根据路径以及相关属性查找文件。
     * 请根据实际情况设置parentPath的值，如/jpk//,/jpk/
     *
     * @param parentPath
     * @param nodeNameLike
     * @param status
     * @param visibility
     * @return
     */
    @Transactional(readOnly = true)
    public List<Document> queryFile(String parentPath, String nodeNameLike,
                                    String status, String visibility) {
        QueryManager qm = mappingTemplate.createQueryManager();
        Filter filter = qm.createFilter(Document.class);
        if (StringUtils.isNotEmpty(parentPath)) {
            filter.setScope(parentPath);
        }
        if (StringUtils.isNotEmpty(nodeNameLike)) {
            String expression = "jcr:like(fn:name(),'%" + nodeNameLike + "%')";
            filter.addJCRExpression(expression);
        }
        if (StringUtils.isNotEmpty(status)) {
            filter.addEqualTo("status", status);
        }
        if (StringUtils.isNotEmpty(visibility)) {
            filter.addEqualTo("visibility", visibility);
        }
        Query q = qm.createQuery(filter);
        q.addOrderByDescending("created");
        return (List<Document>) mappingTemplate.getObjects(q);
    }


    public Map<String, Object> queryTop10File(final String parentPath) {
        return mappingTemplate.execute(new JcrCallback<Map<String, Object>>() {
            @Override
            public Map<String, Object> doInJcr(Session session) throws IOException, RepositoryException {
                String tempPath = parentPath;
                if (!parentPath.startsWith("/")) {
                    tempPath = "/" + parentPath;
                }

                javax.jcr.query.QueryManager qm = session.getWorkspace().getQueryManager();
                StringBuilder sqlsb = new StringBuilder();
                sqlsb.append("SELECT * FROM [rm:file] AS f WHERE ISDESCENDANTNODE(f,'");
                sqlsb.append(tempPath + "') ");

                javax.jcr.query.Query query = qm.createQuery(sqlsb.toString(), javax.jcr.query.Query.JCR_SQL2);
                long total = query.execute().getNodes().getSize();
                System.out.println("list size : " + total);

//                sqlsb.append(" order by [jcr:created] desc");
                query = qm.createQuery(sqlsb.toString(), javax.jcr.query.Query.JCR_SQL2);
                query.setOffset(0);
                query.setLimit(10);

                Map<String, Object> result = new HashMap<>();
                List<Entry> rows = new ArrayList<>();

                QueryResult nodeResult = query.execute();
                for (NodeIterator it = nodeResult.getNodes(); it.hasNext(); ) {
                    Node n = it.nextNode();
                    Entry entry = NodeMapper.mapEntry(n);
                    rows.add(entry);
                }
                result.put("total", total);
                result.put("rows", rows);
                return result;
            }
        });
    }

    public Map<String, Object> queryFile(final String parentPath, final String nameLike,
                                         final long from, final long size) {
        return mappingTemplate.execute(new JcrCallback<Map<String, Object>>() {
            @Override
            public Map<String, Object> doInJcr(Session session) throws IOException, RepositoryException {
                String tempPath = parentPath;
                if (!parentPath.startsWith("/")) {
                    tempPath = "/" + parentPath;
                }

                javax.jcr.query.QueryManager qm = session.getWorkspace().getQueryManager();
                StringBuilder sqlsb = new StringBuilder();
                sqlsb.append("SELECT * FROM [rm:file] AS f WHERE ISDESCENDANTNODE(f,'");
                sqlsb.append(tempPath + "') ");
                if (StringUtils.isNotEmpty(nameLike)) {
                    sqlsb.append("AND LOCALNAME(f) LIKE '%" + nameLike + "%' ");
                }
                javax.jcr.query.Query query = qm.createQuery(sqlsb.toString(), javax.jcr.query.Query.JCR_SQL2);
                long total = query.execute().getNodes().getSize();

                query = qm.createQuery(sqlsb.toString(), javax.jcr.query.Query.JCR_SQL2);
                query.setOffset(from);
                query.setLimit(size);

                Map<String, Object> result = new HashMap<>();
                List<Entry> rows = new ArrayList<>();

                QueryResult nodeResult = query.execute();
                for (NodeIterator it = nodeResult.getNodes(); it.hasNext(); ) {
                    Node n = it.nextNode();
                    Entry entry = NodeMapper.mapEntry(n);
                    rows.add(entry);
                }
                result.put("total", total);
                result.put("rows", rows);
                return result;
            }
        });
    }

    public void deleteFile(String filePath) {
        mappingTemplate.remove(filePath);
        mappingTemplate.save();
    }

    public void rename(String path, String newName) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try {
            Node node = mappingTemplate.getRootNode().getNode(path);
            if (node != null) {
                mappingTemplate.rename(node, newName);
                mappingTemplate.save();
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    @Transactional(readOnly = true)
    public InputStream readFile(final String filePath) {
        return mappingTemplate.execute(new JcrCallback<InputStream>() {
            @Override
            public InputStream doInJcr(Session session) throws IOException, RepositoryException {
                String path = filePath;
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                Node root = session.getRootNode();
                Node node = root.getNode(path);
                String nodeType = node.getPrimaryNodeType().getName();
                if (nodeType.equals(JcrConstants.NT_FILE) ||
                        nodeType.equals(JcrConstants.NT_RESOURCE)) {
                    return JcrUtils.readFile(node);
                } else if (nodeType.equals(Constants.NT_FILE)) {
                    for (NodeIterator ni = node.getNodes(); ni.hasNext(); ) {
                        System.out.println(ni.next());
                    }
                    Node contentNode = node.getNode("{" + Constants.NS_RESMAN + "}fileStream");
                    Property p = contentNode.getProperty("{" + Constants.NS_RESMAN + "}" + "content");
                    final Binary binary = p.getBinary();
                    return new FilterInputStream(binary.getStream()) {
                        public void close() throws IOException {
                            super.close();
                            binary.dispose();
                        }
                    };
                }
                return null;
            }
        });
    }

    @Transactional(readOnly = true)
    public void readFile(String filePath, OutputStream os) {
        try (InputStream input = readFile(filePath)) {
            byte[] buffer = new byte[1024 * 100];

            for (int n = input.read(buffer); n != -1; n = input.read(buffer)) {
                os.write(buffer, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Transactional(readOnly = true)
    public Document getFileInfoByUUID(final String uuid) {
        checkNotNull(uuid);
        return mappingTemplate.execute(new JcrMappingCallback<Document>() {
            @Override
            public Document doInJcrMapping(ObjectContentManager manager) throws JcrMappingException {
                return (Document) manager.getObjectByUuid(uuid);
            }
        });
    }

    @Transactional(readOnly = true)
    public Long countOfFile(final String folder) {
        return mappingTemplate.execute(new JcrCallback<Long>() {
            @Override
            public Long doInJcr(Session session) throws IOException, RepositoryException {
                javax.jcr.query.QueryManager qm = session.getWorkspace().getQueryManager();
                String sql = "";
                if (StringUtils.isNotEmpty(folder)) {
                    sql = "SELECT f.[rm:size] FROM [rm:file] AS f WHERE ISCHILDNODE(f,'" + folder + "')";
                } else {
                    sql = "SELECT f.[rm:size] FROM [rm:file] AS f";
                }
                javax.jcr.query.Query query = qm.createQuery(sql, javax.jcr.query.Query.JCR_SQL2);
                QueryResult nodeResult = query.execute();
                return nodeResult.getNodes().getSize();
            }
        });
    }

    @Transactional(readOnly = true)
    public Long sizeOf(final String folder) {
        return mappingTemplate.execute(new JcrCallback<Long>() {
            @Override
            public Long doInJcr(Session session) throws IOException, RepositoryException {
                javax.jcr.query.QueryManager qm = session.getWorkspace().getQueryManager();
                String sql = "";
                if (StringUtils.isNotEmpty(folder)) {
                    sql = "SELECT f.[rm:size] FROM [rm:file] AS f WHERE ISCHILDNODE(f,'" + folder + "')";
                } else {
                    sql = "SELECT f.[rm:size] FROM [rm:file] AS f";
                }
                javax.jcr.query.Query query = qm.createQuery(sql, javax.jcr.query.Query.JCR_SQL2);
                QueryResult nodeResult = query.execute();
                Long allSize = 0L;
                String[] columns = nodeResult.getColumnNames();
                for (RowIterator it = nodeResult.getRows(); it.hasNext(); ) {
                    allSize += ((it.nextRow().getValue(columns[0]).getLong())) / 1024;
                }
                return allSize;
            }
        });
    }

    public String getParentUid(final String uid) {
        String parentUid = mappingTemplate.execute(new JcrCallback<String>() {
            @Override
            public String doInJcr(Session session) throws IOException, RepositoryException {
                Node node = session.getNodeByIdentifier(uid);
                Node parent = node.getParent();
                return parent.getIdentifier();
            }
        });
        return parentUid;
    }
}
