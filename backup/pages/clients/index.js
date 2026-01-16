import { useEffect, useState } from 'react';
import ClientTable from '../../components/ClientTable';
import ClientFormModal from '../../components/ClientFormModal';
import { getUsers, addUser, updateUser } from '../../services/userService';

export default function ClientsPage() {
    const [clients, setClients] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [modalData, setModalData] = useState(null);

    const pageSize = 10;

    useEffect(() => {
        loadClients();
    }, [page]);

    async function loadClients() {
        const response = await getUsers(page, pageSize);
        setClients(response.content);
        setTotalPages(response.totalPages);
    }

    async function handleAdd(data) {
        await addUser(data);
        setModalData(null);
        loadClients();
    }

    async function handleEdit(data) {
        await updateUser(data);
        setModalData(null);
        loadClients();
    }

    return (
        <>
            <div className="page-header">
                <h2>Clients</h2>
                <button
                    className="primary-btn"
                    onClick={() => setModalData({ mode: 'add' })}
                >
                    + Add Client
                </button>
            </div>

            <ClientTable
                clients={clients}
                page={page}
                pageSize={pageSize}
                onEdit={(client) =>
                    setModalData({ mode: 'edit', client })
                }
            />

            <div className="pagination">
                <button disabled={page === 0} onClick={() => setPage(page - 1)}>
                    Prev
                </button>
                <span>Page {page}</span>
                <button
                    disabled={page + 1 >= totalPages}
                    onClick={() => setPage(page + 1)}
                >
                    Next
                </button>
            </div>

            {modalData && (
                <ClientFormModal
                    title={modalData.mode === 'add' ? 'Add Client' : 'Edit Client'}
                    initialData={modalData.client}
                    onCancel={() => setModalData(null)}
                    onSubmit={
                        modalData.mode === 'add' ? handleAdd : handleEdit
                    }
                />
            )}
        </>
    );
}
