import Link from 'next/link';

export default function Navbar() {
    return (
        <nav className="navbar">
            <div className="nav-left">
                <span className="brand">POS</span>
                <Link href="/clients"><a>Clients</a></Link>
            </div>
        </nav>
    );
}
