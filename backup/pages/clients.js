import { useEffect, useState } from "react";
import { getUsers, addUser, updateUser } from "../services/userService";

export default function Clients() {
    const [users, setUsers] = useState([]);
    const [page] = useState(0);

    const [newName, setNewName] = useState("");
    const [newEmail, setNewEmail] = useState("");

    const [editingId, setEditingId] = useState(null);
    const [editName, setEditName] = useState("");
    const [editEmail, setEditEmail] = useState("");

    useEffect(() => {
        loadUsers();
    }, []);

    async function loadUsers() {
        try {
            const data = await getUsers(page, 10);
            setUsers(data.content);
        } catch (e) {
            alert(e.message);
        }
    }

    async function handleAdd() {
        try {
            await addUser({ name: newName, email: newEmail });
            setNewName("");
            setNewEmail("");
            loadUsers();
        } catch (e) {
            alert(e.message);
        }
    }

    async function handleUpdate(id) {
        try {
            await updateUser({ id, name: editName, email: editEmail });
            setEditingId(null);
            loadUsers();
        } catch (e) {
            alert(e.message);
        }
    }

    return (
        <div style={{ padding: "20px" }}>
            <h1>Client Master</h1>

            {/* Add Client */}
            <h3>Add Client</h3>
            <input
                placeholder="Name"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
            />
            <input
                placeholder="Email"
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
            />
            <button onClick={handleAdd}>Add</button>

            <hr />

            {/* Client Table */}
            <table border="1" cellPadding="8">
                <thead>
                <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Action</th>
                </tr>
                </thead>
                <tbody>
                {users.map((u) => (
                    <tr key={u.id}>
                        <td>
                            {editingId === u.id ? (
                                <input
                                    value={editName}
                                    onChange={(e) => setEditName(e.target.value)}
                                />
                            ) : (
                                u.name
                            )}
                        </td>
                        <td>
                            {editingId === u.id ? (
                                <input
                                    value={editEmail}
                                    onChange={(e) => setEditEmail(e.target.value)}
                                />
                            ) : (
                                u.email
                            )}
                        </td>
                        <td>
                            {editingId === u.id ? (
                                <button onClick={() => handleUpdate(u.id)}>Save</button>
                            ) : (
                                <button
                                    onClick={() => {
                                        setEditingId(u.id);
                                        setEditName(u.name);
                                        setEditEmail(u.email);
                                    }}
                                >
                                    Edit
                                </button>
                            )}
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}