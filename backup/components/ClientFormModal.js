import { useState, useEffect } from 'react';

export default function ClientFormModal({
                                            initialData,
                                            onSubmit,
                                            onCancel,
                                            title,
                                        }) {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');

    useEffect(() => {
        if (initialData) {
            setName(initialData.name || '');
            setEmail(initialData.email || '');
        }
    }, [initialData]);

    function submit() {
        onSubmit({ ...initialData, name, email });
    }

    return (
        <div className="modal-overlay">
            <div className="modal">
                <h3>{title}</h3>

                <input
                    placeholder="Name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                />

                <input
                    placeholder="Email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                />

                <div className="modal-actions">
                    <button onClick={onCancel}>Cancel</button>
                    <button className="primary-btn" onClick={submit}>
                        Submit
                    </button>
                </div>
            </div>
        </div>
    );
}
