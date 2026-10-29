package com.storagepulse.cleaner.data.storage

import com.storagepulse.cleaner.data.storage.model.StorageNode

/** Pure helpers over the in-memory [StorageNode] tree. */
object StorageTree {

    /** Finds the node at [path] (exact match), descending only through matching prefixes. */
    fun find(root: StorageNode, path: String): StorageNode? {
        if (root.path == path) return root
        var node = root
        while (true) {
            val next = node.children.firstOrNull { c ->
                c.isDirectory && (path == c.path || path.startsWith(c.path + "/"))
            } ?: return node.children.firstOrNull { it.path == path }
            if (next.path == path) return next
            node = next
        }
    }

    /** Parent folder path, or null at the root. */
    fun parentPath(root: StorageNode, path: String): String? =
        if (path == root.path || !path.startsWith(root.path)) null else path.substringBeforeLast('/')

    /**
     * Returns a new tree without the given files ([removed] = path → size). Only folders on
     * the way to a removed file are rebuilt; sizes and counts stay exact.
     */
    fun withoutFiles(root: StorageNode, removed: Map<String, Long>): StorageNode {
        if (removed.isEmpty()) return root
        val bytesUnder = HashMap<String, Long>()
        val filesUnder = HashMap<String, Int>()
        for ((path, size) in removed) {
            var dir = path.substringBeforeLast('/')
            while (dir.length >= root.path.length) {
                bytesUnder[dir] = (bytesUnder[dir] ?: 0L) + size
                filesUnder[dir] = (filesUnder[dir] ?: 0) + 1
                if (dir == root.path) break
                dir = dir.substringBeforeLast('/')
            }
        }

        fun rebuild(node: StorageNode): StorageNode {
            val lostBytes = bytesUnder[node.path] ?: return node
            val children = node.children
                .filterNot { !it.isDirectory && it.path in removed }
                .map { if (it.isDirectory) rebuild(it) else it }
                .sortedByDescending { it.sizeBytes }
            return node.copy(
                sizeBytes = (node.sizeBytes - lostBytes).coerceAtLeast(0),
                fileCount = (node.fileCount - (filesUnder[node.path] ?: 0)).coerceAtLeast(0),
                children = children,
            )
        }
        return rebuild(root)
    }
}
