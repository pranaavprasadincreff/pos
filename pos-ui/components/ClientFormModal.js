import { useState } from 'react';
import { addUser } from '../services/userService';

export default function AddClientModal({ onCancel, onSuccess }) {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');

    async function submit() {
        await addUser({ name, email });
        onSuccess();
    }

    return (
        <div className="modal-overlay">
            <div className="modal">
                <h3>Add Client</h3>

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
