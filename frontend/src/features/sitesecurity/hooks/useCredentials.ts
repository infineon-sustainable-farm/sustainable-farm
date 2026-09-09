import { useEffect, useState } from "react";
import { createCredentialApi, fetchCredentials } from "../api/sitesecurityApi";
import type { User } from "../api/sitesecurityTypes";

export function useCredentials() {
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [freshId, setFreshId] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    fetchCredentials().then((res) => {
      if (isMounted) {
        setUsers(res);
        setIsLoading(false);
      }
    });
    return () => {
      isMounted = false;
    };
  }, []);

  const addCredential = async (u: User) => {
    const created = await createCredentialApi(u);
    setUsers((prev) => [created, ...prev]);
    setFreshId(created.id);
  };

  return { users, freshId, addCredential, isLoading };
}
