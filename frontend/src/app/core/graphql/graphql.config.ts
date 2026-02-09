import { provideApollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';
import { InMemoryCache, ApolloLink } from '@apollo/client/core';
import { setContext } from '@apollo/client/link/context';
import { inject } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';

export function provideGraphQL() {
  return provideApollo(() => {
    const httpLink = inject(HttpLink);

    const auth = setContext((operation, context) => {
      const token = localStorage.getItem('access_token');
      if (!token) {
        return {};
      }
      return {
        headers: new HttpHeaders().set('Authorization', `Bearer ${token}`),
      };
    });

    const link = ApolloLink.from([
      auth,
      httpLink.create({ uri: 'http://localhost:8081/graphql' })
    ]);

    return {
      link,
      cache: new InMemoryCache(),
      defaultOptions: {
        watchQuery: {
          fetchPolicy: 'network-only',
        },
      },
    };
  });
}
