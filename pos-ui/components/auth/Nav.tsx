const role = sessionStorage.getItem("auth.role")

{canView(role, "clients") && <NavLink to="/clients">Clients</NavLink>}
{canView(role, "products") && <NavLink to="/products">Products</NavLink>}
{canView(role, "orders") && <NavLink to="/orders">Orders</NavLink>}
{canView(role, "reports") && <NavLink to="/reports">Sales Report</NavLink>}
