export default function ClientTable({ clients, page, pageSize, onEdit }) {
    return (
        <table className="table">
            <thead>
            <tr>
                <th>SNo</th>
                <th>Name</th>
                <th>Email</th>
                <th></th>
            </tr>
            </thead>

            <tbody>
            {clients.map((c, index) => (
                <tr key={c.id}>
                    <td>{page * pageSize + index + 1}</td>
                    <td>{c.name}</td>
                    <td>{c.email}</td>
                    <td>
                        <button onClick={() => onEdit(c)}>Edit</button>
                    </td>
                </tr>
            ))}
            </tbody>
        </table>
    );
}
